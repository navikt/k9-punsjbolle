package no.nav.punsjbolle.innsending

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.punsjbolle.leggTilLøsningPar
import no.nav.punsjbolle.meldinger.HentAktørIderMelding
import org.slf4j.LoggerFactory

internal class PunsjetSøknadBehandlingRiver(rapidsConnection: RapidsConnection) : BehovssekvensPacketListener(
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

        if (søknad.saksnummer == null) {
            logger.info("TODO: Hente saksnummer")
        }

        repeat(søknad.journalpostIder.size) { journalpostId ->
            logger.info("TODO: Hente journalpostInfo for $journalpostId")
        }

        logger.info("TODO: Sende søknad til K9-sak. K9-sak støtter bare en jpid...")

        packet.leggTilLøsningPar(PunsjetSøknadMelding.løsning(søknad.saksnummer!!))

        return true
    }
}