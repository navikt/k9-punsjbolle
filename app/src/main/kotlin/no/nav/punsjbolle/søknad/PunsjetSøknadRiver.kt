package no.nav.punsjbolle.søknad

import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import no.nav.k9.rapid.river.leggTilBehov
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.k9.rapid.river.utenLøsningPåBehov
import no.nav.punsjbolle.CorrelationId.Companion.correlationId
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.joark.SafClient
import no.nav.punsjbolle.meldinger.HentAktørIderMelding
import org.slf4j.LoggerFactory

internal class PunsjetSøknadRiver(
    rapidsConnection: RapidsConnection,
    private val safClient: SafClient
) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(PunsjetSøknadRiver::class.java),
    mdcPaths = PunsjetSøknadMelding.mdcPaths) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(PunsjetSøknadMelding.behovNavn)
                it.utenLøsningPåBehov(HentAktørIderMelding.behovNavn)
                PunsjetSøknadMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun doHandlePacket(id: String, packet: JsonMessage): Boolean {
        val correlationId = packet.correlationId()
        val journalpostId = packet.hentJournalpost()
        val journalpost = runBlocking { safClient.hentJournalpost(JournalpostId(journalpostId), correlationId) }
        val søknad = PunsjetSøknadMelding.hentBehov(packet, journalpost.brevkode)
        val erStøttetVersjon = søknad.versjon in StøttedeVersjoner
        logger.info("Søknadstype=[${søknad.søknadstype.name}], Versjon=[${søknad.versjon}], ErStøttetVersjon=[$erStøttetVersjon]")
        return erStøttetVersjon
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val correlationId = packet.correlationId()
        val journalpostId = packet.hentJournalpost()
        val journalpost = runBlocking { safClient.hentJournalpost(JournalpostId(journalpostId), correlationId) }
        val søknad = PunsjetSøknadMelding.hentBehov(packet, journalpost.brevkode)

        logger.info("Legger til behov for å hente aktørId på de involverte partene.")
        packet.leggTilBehov(PunsjetSøknadMelding.behovNavn, HentAktørIderMelding.behov(
            behovInput = søknad.identitetsnummer
        ))

        return true
    }

    private fun JsonMessage.hentJournalpost(): String {
        val søknadJson = this[PunsjetSøknadMelding.SøknadKey] as ObjectNode
        return søknadJson.get("journalposter").first().get("journalpostId").asText()
    }

    private companion object {
        val StøttedeVersjoner = setOf("1.0.0")
    }
}
