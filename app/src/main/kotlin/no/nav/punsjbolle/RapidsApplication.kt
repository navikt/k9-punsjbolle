package no.nav.punsjbolle

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.k9.rapid.river.RapidsStateListener
import no.nav.punsjbolle.søknad.PunsjetSøknadBehandlingRiver
import no.nav.punsjbolle.søknad.PunsjetSøknadRiver

internal fun RapidsConnection.registerApplicationContext(applicationContext: ApplicationContext) {
    PunsjetSøknadRiver(rapidsConnection = this)
    PunsjetSøknadBehandlingRiver(rapidsConnection = this, k9SakClient = applicationContext.k9SakClient, safClient = applicationContext.safClient)

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