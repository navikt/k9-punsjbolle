package no.nav.punsjbolle.søknad

import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.punsjbolle.CorrelationId.Companion.correlationId
import no.nav.punsjbolle.k9sak.K9SakClient
import no.nav.punsjbolle.meldinger.FerdigstillJournalføringForK9Melding
import no.nav.punsjbolle.meldinger.SendSøknadTilK9SakMelding
import org.slf4j.LoggerFactory

internal class PunsjetSøknadInnsendingRiver(
    rapidsConnection: RapidsConnection,
    private val k9SakClient: K9SakClient) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(PunsjetSøknadInnsendingRiver::class.java),
    mdcPaths = PunsjetSøknadMelding.mdcPaths) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(SendSøknadTilK9SakMelding.behovNavn)
                it.harLøsningPåBehov(FerdigstillJournalføringForK9Melding.behovNavn)
                SendSøknadTilK9SakMelding.validateBehov(it)
                PunsjetSøknadMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val correlationId = packet.correlationId()
        val søknad = PunsjetSøknadMelding.hentBehov(packet)
        val grunnlag = SendSøknadTilK9SakMelding.hentBehov(packet)

        runBlocking { k9SakClient.sendInnSøknad(
            søknad = søknad,
            grunnlag = grunnlag,
            correlationId = correlationId
        )}

        packet.leggTilLøsning(behov = SendSøknadTilK9SakMelding.behovNavn) // TODO: SendPunsjet?2
        packet.leggTilLøsning(behov = PunsjetSøknadMelding.behovNavn)

        return true
    }
}