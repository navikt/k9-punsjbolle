package no.nav.punsjbolle.søknad

import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.river.leggTilBehov
import no.nav.k9.rapid.river.leggTilBehovMedLøsninger
import no.nav.punsjbolle.CorrelationId.Companion.correlationId
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.joark.Journalpost
import no.nav.punsjbolle.joark.SafClient
import no.nav.punsjbolle.k9sak.K9SakClient
import no.nav.punsjbolle.meldinger.FerdigstillJournalføringForK9Melding
import no.nav.punsjbolle.meldinger.HentAktørIderMelding
import no.nav.punsjbolle.meldinger.HentK9SaksnummerMelding
import no.nav.punsjbolle.meldinger.JournalførJsonMelding
import no.nav.punsjbolle.meldinger.SendPunsjetSøknadTilK9SakMelding
import org.slf4j.LoggerFactory

internal class PunsjetSøknadTilK9Sak(
    private val k9SakClient: K9SakClient,
    private val safClient: SafClient) {

    internal fun handlePacket(packet: JsonMessage): Boolean {
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
            null -> runBlocking { k9SakClient.hentEllerOpprettSaksnummer(
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

        logger.info("Brevkoder=${journalposter.map { it.brevkode }}")

        val ferdigstillJournalføringBehov = FerdigstillJournalføringForK9Melding.behov(
            Triple(søknad.søker, k9Saksnummer, journalpostIderSomMåFerdigstilles(
                journalposter = journalposter,
                saksnummer = k9Saksnummer
            ))
        )

        val journalførJsonBehov = JournalførJsonMelding.behov(
            JournalførJsonMelding.JournalførJson(
                punsjetSøknad = søknad,
                saksnummer = k9Saksnummer
            )
        )

        val innsendingBehov = SendPunsjetSøknadTilK9SakMelding.behov(
            behovInput = k9Saksnummer
        )

        logger.info("Legger til behov for ferdigstilling av journalpost, journalføring av JSON og innsending.")
        packet.leggTilBehov(PunsjetSøknadMelding.behovNavn,
            ferdigstillJournalføringBehov, journalførJsonBehov, innsendingBehov
        )

        return true
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(PunsjetSøknadTilK9Sak::class.java)

        internal fun journalpostIderSomMåFerdigstilles(journalposter: Set<Journalpost>, saksnummer: K9Saksnummer) : Set<JournalpostId> {
            val erFerdigstiltMotSak = journalposter.filter { it.erKnyttetTil(saksnummer)}.filter { it.erFerdigstilt }.also { if (it.isNotEmpty()) {
                logger.info("Allerede ferdigstilt mot sak. K9Saksnummer=[$saksnummer], JournalpostIder=${it.map { journalpost ->  journalpost.journalpostId }}")
            }}

            val erKnyttetMotSakIkkeFerdigstilt = journalposter.filter { it.erKnyttetTil(saksnummer)}.filterNot { it.erFerdigstilt }.also { if (it.isNotEmpty()) {
                logger.info("Allerede knyttet mot sak, men mangler ferdigstilling. K9Saksnummer=[$saksnummer], Journalposter=$it")
            }}

            val erMottattInngåndeJournalpostSomMåFerdigstilles = journalposter.filter { it.kanKnyttesTilSak }.also { if (it.isNotEmpty()) {
                logger.info("Mottatt inngående journalposter som må ferdigstilles. K9Saksnummer=[$saksnummer], JournalpostIder=${it.map { journalpost -> journalpost.journalpostId }}")
            }}

            journalposter.minus(erMottattInngåndeJournalpostSomMåFerdigstilles).minus(erKnyttetMotSakIkkeFerdigstilt).minus(erFerdigstiltMotSak).also { if (it.isNotEmpty()) {
                throw IllegalStateException("Inneholder journalposter som ikke støttes for K9Saksnummer=[$saksnummer]. Journalposter=$it")
            }}

            return erKnyttetMotSakIkkeFerdigstilt.plus(erMottattInngåndeJournalpostSomMåFerdigstilles).map { it.journalpostId }.toSet()
        }
    }
}