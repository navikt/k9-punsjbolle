package no.nav.punsjbolle.søknad

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.behov.Behovsformat
import no.nav.k9.rapid.river.*
import no.nav.punsjbolle.CorrelationId.Companion.somCorrelationId
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.joark.SafClient
import no.nav.punsjbolle.joark.Journalpost
import no.nav.punsjbolle.joark.Journalpost.Companion.tidligstMottattJournalpost
import no.nav.punsjbolle.k9sak.K9SakClient
import no.nav.punsjbolle.leggTilLøsningPar
import no.nav.punsjbolle.meldinger.HentAktørIderMelding
import no.nav.punsjbolle.meldinger.HentK9SaksnummerMelding
import org.slf4j.LoggerFactory

internal class PunsjetSøknadBehandlingRiver(
    rapidsConnection: RapidsConnection,
    private val k9SakClient: K9SakClient,
    private val safClient: SafClient) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(PunsjetSøknadBehandlingRiver::class.java),
    mdcPaths = PunsjetSøknadMelding.mdcPaths) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(PunsjetSøknadMelding.behovNavn)
                it.harLøsningPåBehov(HentAktørIderMelding.løsningNavn)
                PunsjetSøknadMelding.validateBehov(it)
                HentAktørIderMelding.validateLøsning(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val søknad = PunsjetSøknadMelding.hentBehov(packet)
        val aktørIder = HentAktørIderMelding.hentLøsning(packet)
        val correlationId = packet[Behovsformat.CorrelationId].asText().somCorrelationId()

        val hentK9SaksnummerGrunnlag = HentK9SaksnummerMelding.HentK9SaksnummerGrunnlag(
            søknadstype = søknad.søknadstype,
            søker = aktørIder.getValue(søknad.søker),
            pleietrengende = søknad.pleietrengende?.let { aktørIder.getValue(it) },
            annenPart = søknad.annenPart?.let { aktørIder.getValue(it) },
            fraOgMed = søknad.fraOgMed,
            tilOgMed = søknad.tilOgMed
        )

        val (k9Saksnummer, k9SaksnummerKilde) = when (søknad.saksnummer) {
            null -> k9SakClient.hentSaksnummer(
                grunnlag = hentK9SaksnummerGrunnlag,
                correlationId = correlationId
            ) to HentK9SaksnummerMelding.K9SaksnummerKilde.SlåttOppMotK9Sak
            else -> søknad.saksnummer to HentK9SaksnummerMelding.K9SaksnummerKilde.ManueltValgtIPunsj
        }
        logger.info("Håndteres på K9Saksnummer=[$k9Saksnummer], Kilde=[${k9SaksnummerKilde.name}]")

        packet.leggTilBehovMedLøsninger(PunsjetSøknadMelding.behovNavn, HentK9SaksnummerMelding.behovMedLøsning(
            behovInput = hentK9SaksnummerGrunnlag,
            løsning = k9Saksnummer to k9SaksnummerKilde
        ))

        val journalposter = safClient.hentJournalposter(
            journalpostIder = søknad.journalpostIder,
            correlationId = correlationId
        )

        val journalpostIderSomSkalKnyttesTilSak = journalpostIderSomSkalKnyttesTilSak(
            journalposter = journalposter,
            saksnummer = k9Saksnummer
        )

        // TODO: Knytt til sak

        k9SakClient.sendInnSøknad(
            søknad = søknad,
            saksnummer = k9Saksnummer,
            correlationId = correlationId,
            journalpost = journalposter.tidligstMottattJournalpost().also {
                logger.info("Sendes til K9Sak med JournalpostId=[${it.journalpostId}]")
            }
        )

        packet.leggTilLøsningPar(PunsjetSøknadMelding.løsning(k9Saksnummer))

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