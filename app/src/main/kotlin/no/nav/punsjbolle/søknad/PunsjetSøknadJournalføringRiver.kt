package no.nav.punsjbolle.søknad

import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.punsjbolle.CorrelationId.Companion.correlationId
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.joark.SafClient
import no.nav.punsjbolle.joark.Journalpost
import no.nav.punsjbolle.k9sak.K9SakClient
import no.nav.punsjbolle.meldinger.FerdigstillJournalføringForK9Melding
import no.nav.punsjbolle.meldinger.HentAktørIderMelding
import no.nav.punsjbolle.meldinger.HentK9SaksnummerMelding
import no.nav.punsjbolle.meldinger.SendPunsjetSøknadTilK9SakMelding
import no.nav.punsjbolle.meldinger.SendPunsjetSøknadTilK9SakMelding.SendPunsjetSøknadTilK9SakGrunnlag.Companion.somSendSøknadTilK9SakGrunnlag
import org.slf4j.LoggerFactory

internal class PunsjetSøknadJournalføringRiver(
    rapidsConnection: RapidsConnection,
    private val k9SakClient: K9SakClient,
    private val safClient: SafClient) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(PunsjetSøknadJournalføringRiver::class.java),
    mdcPaths = PunsjetSøknadMelding.mdcPaths) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(PunsjetSøknadMelding.behovNavn)
                it.harLøsningPåBehov(HentAktørIderMelding.behovNavn)
                PunsjetSøknadMelding.validateBehov(it)
                HentAktørIderMelding.validateLøsning(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val søknad = PunsjetSøknadMelding.hentBehov(packet)
        val aktørIder = HentAktørIderMelding.hentLøsning(packet)
        val correlationId = packet.correlationId()

        val hentK9SaksnummerGrunnlag = HentK9SaksnummerMelding.HentK9SaksnummerGrunnlag(
            søknadstype = søknad.søknadstype,
            søker = aktørIder.getValue(søknad.søker),
            pleietrengende = søknad.pleietrengende?.let { aktørIder.getValue(it) },
            annenPart = søknad.annenPart?.let { aktørIder.getValue(it) },
            periode = søknad.periode
        )

        val (k9Saksnummer, k9SaksnummerKilde) = when (søknad.saksnummer) {
            null -> runBlocking { k9SakClient.hentSaksnummer(
                grunnlag = hentK9SaksnummerGrunnlag,
                correlationId = correlationId
            ) to HentK9SaksnummerMelding.K9SaksnummerKilde.SlåttOppMotK9Sak }
            else -> søknad.saksnummer to HentK9SaksnummerMelding.K9SaksnummerKilde.ManueltValgtIPunsj
        }
        logger.info("Håndteres på K9Saksnummer=[$k9Saksnummer], Kilde=[${k9SaksnummerKilde.name}]")

        packet.leggTilBehovMedLøsninger(PunsjetSøknadMelding.behovNavn, HentK9SaksnummerMelding.behovMedLøsning(
            behovInput = hentK9SaksnummerGrunnlag,
            løsning = k9Saksnummer to k9SaksnummerKilde
        ))

        val journalposter = runBlocking { safClient.hentJournalposter(
            journalpostIder = søknad.journalpostIder,
            correlationId = correlationId
        )}

        val journalføringBehov = FerdigstillJournalføringForK9Melding.behov(
            Triple(søknad.søker, k9Saksnummer, journalpostIderSomSkalKnyttesTilSak(
                journalposter = journalposter,
                saksnummer = k9Saksnummer
            ))
        )

        val innsendingBehov = SendPunsjetSøknadTilK9SakMelding.behov(
            journalposter.somSendSøknadTilK9SakGrunnlag(k9Saksnummer)
        )

        packet.leggTilBehov(PunsjetSøknadMelding.behovNavn,
            journalføringBehov, innsendingBehov
        )

        return true
    }

    private fun journalpostIderSomSkalKnyttesTilSak(journalposter: Set<Journalpost>, saksnummer: K9Saksnummer) : Set<JournalpostId> {
        val erKnyttetTilSak = journalposter.filter { it.erKnyttetTil(saksnummer) }.also { if (it.isNotEmpty()) {
            logger.info("Allerede knyttet til sak. K9Saksnummer=[$saksnummer], JournalpostIder=${journalposter.map { it.journalpostId }}")
        }}

        val skalKnyttesTilSak = journalposter.filter { it.skalKnyttesTilSak() }.also { if (it.isNotEmpty()) {
            logger.info("Skal knyttes til sak. K9Saksnummer=[$saksnummer], JournalpostIder=${journalposter.map { it.journalpostId }}")
        }}

        journalposter.minus(erKnyttetTilSak).minus(skalKnyttesTilSak).also { if (it.isNotEmpty()) {
            throw IllegalStateException("Inneholder journalposter som ikke støttes. Journalposter=$it")
        }}

        return skalKnyttesTilSak.map { it.journalpostId }.toSet()
    }
}