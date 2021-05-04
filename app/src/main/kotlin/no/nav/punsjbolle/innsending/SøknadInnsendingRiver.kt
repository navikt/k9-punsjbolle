package no.nav.punsjbolle.innsending

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import org.slf4j.LoggerFactory

class SøknadInnsendingRiver(rapidsConnection: RapidsConnection) : BehovssekvensPacketListener(logger = LoggerFactory.getLogger(SøknadInnsendingRiver::class.java)) {

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        return true
    }
}