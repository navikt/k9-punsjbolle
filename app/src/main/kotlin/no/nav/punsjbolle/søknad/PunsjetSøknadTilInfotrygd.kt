package no.nav.punsjbolle.søknad

import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.river.leggTilBehovEtter
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.punsjbolle.CorrelationId.Companion.correlationId
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.joark.Journalpost
import no.nav.punsjbolle.joark.SafClient
import no.nav.punsjbolle.meldinger.OpprettGosysJournalføringsoppgaverMelding
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class PunsjetSøknadTilInfotrygd(
    private val safClient: SafClient) {

    internal fun handlePacket(packet: JsonMessage): Boolean {
        val søknad = PunsjetSøknadMelding.hentBehov(packet)

        val journalpostIder = runBlocking { safClient.hentJournalposter(
            journalpostIder = søknad.journalpostIder,
            correlationId = packet.correlationId()
        )}.journalpostIderSomDetSkalOpprettesGosysJournalføringsoppgaverFor()

        packet.leggTilLøsning(behov = PunsjetSøknadMelding.behovNavn)
        packet.leggTilBehovEtter(aktueltBehov = PunsjetSøknadMelding.behovNavn, OpprettGosysJournalføringsoppgaverMelding.behov(
            søknad to journalpostIder
        ))

        return true
    }

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PunsjetSøknadTilInfotrygd::class.java)

        private fun Set<Journalpost>.journalpostIderSomDetSkalOpprettesGosysJournalføringsoppgaverFor() : Set<JournalpostId> {
            val skalOpprettesGosysJournalføringsoppgaverFor = filter { it.kanKnyttesTilSak }.also { if (it.isNotEmpty()) {
                logger.info("Skal opprett Gosys journalføringsoppgaver, JournalpostIder=${map { journalpost -> journalpost.journalpostId }}")
            }}

            minus(skalOpprettesGosysJournalføringsoppgaverFor).also { if (it.isNotEmpty()) {
                throw IllegalStateException("Inneholder journalposter som ikke støttes. Journalposter=$it")
            }}

            return skalOpprettesGosysJournalføringsoppgaverFor.map { it.journalpostId }.toSet()
        }
    }
}