package no.nav.punsjbolle.søknad

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import no.nav.k9.rapid.river.harLøsningPåBehov
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.punsjbolle.joark.SafClient
import no.nav.punsjbolle.k9sak.K9SakClient
import no.nav.punsjbolle.meldinger.HentAktørIderMelding
import org.slf4j.LoggerFactory

internal class PunsjetSøknadJournalføringRiver(
    rapidsConnection: RapidsConnection,
    k9SakClient: K9SakClient,
    safClient: SafClient,
) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(PunsjetSøknadJournalføringRiver::class.java),
    mdcPaths = PunsjetSøknadMelding.mdcPaths) {

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
        return punsjetSøknadTilK9Sak.handlePacket(packet)
    }

}