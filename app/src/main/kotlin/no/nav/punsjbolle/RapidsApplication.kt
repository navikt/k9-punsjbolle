package no.nav.punsjbolle

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.k9.rapid.river.RapidsStateListener
import no.nav.punsjbolle.innsending.PunsjetSøknadRiver

internal fun RapidsConnection.registerApplicationContext(applicationContext: ApplicationContext) {
    PunsjetSøknadRiver(rapidsConnection = this)

    register(object : RapidsConnection.StatusListener {
        override fun onStartup(rapidsConnection: RapidsConnection) {
            applicationContext.start()
        }
        override fun onShutdown(rapidsConnection: RapidsConnection) {
            applicationContext.stop()
        }
    })

    register(RapidsStateListener(onStateChange = { state -> applicationContext.rapidsState = state }))
}