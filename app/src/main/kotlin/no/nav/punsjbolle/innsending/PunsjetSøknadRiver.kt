package no.nav.punsjbolle.innsending

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import no.nav.k9.rapid.river.leggTilBehov
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.k9.rapid.river.utenLøsningPåBehov
import no.nav.punsjbolle.meldinger.HentAktørIderMelding
import org.slf4j.LoggerFactory

internal class PunsjetSøknadRiver(rapidsConnection: RapidsConnection) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(PunsjetSøknadRiver::class.java),
    mdcPaths = PunsjetSøknadMelding.mdcPaths) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(PunsjetSøknadMelding.behovNavn)
                it.utenLøsningPåBehov(HentAktørIderMelding.løsningNavn)
                PunsjetSøknadMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun doHandlePacket(id: String, packet: JsonMessage): Boolean {
        val søknad = PunsjetSøknadMelding.hentBehov(packet)
        val erStøttetVersjon = søknad.versjon in StøttedeVersjoner
        logger.info("Søknadstype=[${søknad.søknadstype.name}], Versjon=[${søknad.versjon}], ErStøttetVersjon=[$erStøttetVersjon]")
        return erStøttetVersjon
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val søknad = PunsjetSøknadMelding.hentBehov(packet)

        packet.leggTilBehov(PunsjetSøknadMelding.behovNavn, HentAktørIderMelding.behov(
            behovInput = søknad.identitetsnummer
        ))

        return true
    }

    private companion object {
        val StøttedeVersjoner = setOf("1.0.0")
    }
}