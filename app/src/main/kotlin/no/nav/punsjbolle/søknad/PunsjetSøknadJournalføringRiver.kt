package no.nav.punsjbolle.søknad

import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.punsjbolle.CorrelationId.Companion.correlationId
import no.nav.punsjbolle.joark.SafClient
import no.nav.punsjbolle.k9sak.K9SakClient
import no.nav.punsjbolle.meldinger.HentAktørIderMelding
import no.nav.punsjbolle.ruting.RutingService
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class PunsjetSøknadJournalføringRiver(
    rapidsConnection: RapidsConnection,
    k9SakClient: K9SakClient,
    safClient: SafClient,
    private val rutingService: RutingService) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(PunsjetSøknadJournalføringRiver::class.java),
    mdcPaths = PunsjetSøknadMelding.mdcPaths) {

    private val punsjetSøknadTilK9Sak = PunsjetSøknadTilK9Sak(
        k9SakClient = k9SakClient,
        safClient = safClient
    )
    private val punsjetSøknadTilInfotrygd = PunsjetSøknadTilInfotrygd(
        safClient = safClient
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
        val søknad = PunsjetSøknadMelding.hentBehov(packet)
        val aktørIder = HentAktørIderMelding.hentLøsning(packet)
        val correlationId = packet.correlationId()

        val destinasjon = runBlocking { rutingService.destinasjon(
            søker = søknad.søker to aktørIder.getValue(søknad.søker),
            fraOgMed = søknad.periode.fom ?: LocalDate.now(),
            pleietrengende = søknad.pleietrengende?.let { it to aktørIder.getValue(it) },
            annenPart = søknad.annenPart?.let { it to aktørIder.getValue(it) },
            correlationId = correlationId
        )}.also { logger.info("Destinasjon=[$it]") }

        return when (destinasjon) {
            RutingService.Destinasjon.K9Sak -> punsjetSøknadTilK9Sak.handlePacket(id, packet)
            RutingService.Destinasjon.Infotrygd -> punsjetSøknadTilInfotrygd.handlePacket(id, packet)
        }
    }
}