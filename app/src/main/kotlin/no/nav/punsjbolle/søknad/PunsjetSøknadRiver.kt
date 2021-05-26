package no.nav.punsjbolle.søknad

import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import no.nav.k9.rapid.river.leggTilBehov
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.k9.rapid.river.utenLøsningPåBehov
import no.nav.punsjbolle.CorrelationId.Companion.correlationId
import no.nav.punsjbolle.joark.SafClient
import no.nav.punsjbolle.meldinger.HentAktørIderMelding
import no.nav.punsjbolle.ruting.RutingService
import org.slf4j.LoggerFactory
import java.time.LocalDate

import no.nav.punsjbolle.ruting.RutingService.Destinasjon.Infotrygd
import no.nav.punsjbolle.ruting.RutingService.Destinasjon.K9Sak

internal class PunsjetSøknadRiver(
    rapidsConnection: RapidsConnection,
    safClient: SafClient,
    private val rutingService: RutingService) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(PunsjetSøknadRiver::class.java),
    mdcPaths = PunsjetSøknadMelding.mdcPaths) {

    private val punsjetSøknadTilInfotrygd = PunsjetSøknadTilInfotrygd(
        safClient = safClient
    )

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
        val søknad = kotlin.runCatching { PunsjetSøknadMelding.hentBehov(packet) }.fold(
            onSuccess = { it },
            onFailure = { throwable ->
                secureLogger.error("Ugyldig søknad. ErrorPacket=${packet.toJson()}", throwable)
                null
            }
        )?:return false

        val erStøttetVersjon = søknad.versjon in StøttedeVersjoner
        logger.info("Søknadstype=[${søknad.søknadstype.name}], Versjon=[${søknad.versjon}], ErStøttetVersjon=[$erStøttetVersjon]")
        return erStøttetVersjon
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val søknad = PunsjetSøknadMelding.hentBehov(packet)

        val destinasjon = runBlocking { rutingService.destinasjon(
            søker = søknad.søker,
            fraOgMed = søknad.periode.fom ?: LocalDate.now(),
            pleietrengende = søknad.pleietrengende,
            annenPart = søknad.annenPart,
            søknadstype = søknad.søknadstype,
            correlationId = packet.correlationId()
        )}.also { logger.info("Destinasjon=[${it.name}]") }

        return when (destinasjon) {
            Infotrygd -> punsjetSøknadTilInfotrygd.handlePacket(packet)
            K9Sak -> {
                logger.info("Legger til behov for å hente aktørId på de involverte partene.")
                packet.leggTilBehov(PunsjetSøknadMelding.behovNavn, HentAktørIderMelding.behov(
                    behovInput = søknad.identitetsnummer
                ))
                true
            }
        }
    }

    private companion object {
        val StøttedeVersjoner = setOf("1.0.0")
    }
}