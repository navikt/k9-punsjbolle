package no.nav.punsjbolle.søknad

import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.punsjbolle.CorrelationId.Companion.correlationId
import no.nav.punsjbolle.k9sak.K9SakClient
import no.nav.punsjbolle.meldinger.FerdigstillJournalføringForK9Melding
import no.nav.punsjbolle.meldinger.JournalførJsonMelding
import no.nav.punsjbolle.meldinger.SendPunsjetSøknadTilK9SakMelding
import org.slf4j.LoggerFactory

internal class PunsjetSøknadInnsendingRiver(
    rapidsConnection: RapidsConnection,
    private val k9SakClient: K9SakClient) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(PunsjetSøknadInnsendingRiver::class.java),
    mdcPaths = PunsjetSøknadMelding.mdcPaths) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(SendPunsjetSøknadTilK9SakMelding.behovNavn)
                it.harLøsningPåBehov(FerdigstillJournalføringForK9Melding.behovNavn)
                SendPunsjetSøknadTilK9SakMelding.validateBehov(it)
                PunsjetSøknadMelding.validateBehov(it)
                JournalførJsonMelding.validateLøsning(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val correlationId = packet.correlationId()
        val søknad = PunsjetSøknadMelding.hentBehov(packet, null)
        val saksnummer = SendPunsjetSøknadTilK9SakMelding.hentBehov(packet, null)
        val journalpostId = JournalførJsonMelding.hentLøsning(packet)

        logger.info("Innsending fra Punsj journalført med JournalpostId=[$journalpostId]")

        runBlocking { k9SakClient.sendInnSøknad(
            søknad = søknad,
            grunnlag = SendPunsjetSøknadTilK9SakMelding.SendPunsjetSøknadTilK9SakGrunnlag(
                saksnummer = saksnummer,
                journalpostId = journalpostId,
                referanse = id
            ),
            correlationId = correlationId
        )}

        logger.info("Søknad sendt OK til K9Sak")

        packet.leggTilLøsning(behov = SendPunsjetSøknadTilK9SakMelding.behovNavn)
        packet.leggTilLøsning(behov = PunsjetSøknadMelding.behovNavn)

        return true
    }
}
