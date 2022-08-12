package no.nav.punsjbolle.søknad

import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.punsjbolle.CorrelationId.Companion.correlationId
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.joark.SafClient
import no.nav.punsjbolle.k9sak.K9SakClient
import no.nav.punsjbolle.meldinger.HentAktørIderMelding
import no.nav.punsjbolle.ruting.RutingService
import org.slf4j.LoggerFactory

internal class PunsjetSøknadJournalføringRiver(
    rapidsConnection: RapidsConnection,
    k9SakClient: K9SakClient,
    private val safClient: SafClient,
    private val rutingService: RutingService) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(PunsjetSøknadJournalføringRiver::class.java),
    mdcPaths = PunsjetSøknadMelding.mdcPaths) {

    private val punsjetSøknadTilInfotrygd = PunsjetSøknadTilInfotrygd(
        safClient = safClient
    )
    private val punsjetSøknadTilK9Sak = PunsjetSøknadTilK9Sak(
        safClient = safClient,
        k9SakClient = k9SakClient
    )

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
        val correlationId = packet.correlationId()
        val journalpostId = packet.hentJournalpost()
        val journalpost = runBlocking { safClient.hentJournalpost(JournalpostId(journalpostId), correlationId) }
        logger.info("Hentet journalpostinfo: {}", journalpost)
        val søknad = PunsjetSøknadMelding.hentBehov(packet, journalpost.brevkode)
        val aktørIder = HentAktørIderMelding.hentLøsning(packet)

        val destinasjon = runBlocking { rutingService.destinasjon(
            søker = søknad.søker,
            fraOgMed = søknad.periode.fom ?: søknad.mottatt.toLocalDate(),
            pleietrengende = søknad.pleietrengende,
            annenPart = søknad.annenPart,
            søknadstype = søknad.søknadstype,
            aktørIder = aktørIder.values.toSet(),
            correlationId = correlationId,
            journalpostIds = søknad.journalpostIder
        )}.also { logger.info("Destinasjon=[${it.name}]") }

        return when (destinasjon) {
            RutingService.Destinasjon.Infotrygd -> punsjetSøknadTilInfotrygd.handlePacket(packet)
            RutingService.Destinasjon.K9Sak ->  punsjetSøknadTilK9Sak.handlePacket(packet)
        }
    }

    private fun JsonMessage.hentJournalpost(): String {
        val søknadJson = this[PunsjetSøknadMelding.SøknadKey] as ObjectNode
        return søknadJson.get("journalposter").first().get("journalpostId").asText()
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(PunsjetSøknadJournalføringRiver::class.java)
    }

}
