package no.nav.punsjbolle

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val applicationContext = ApplicationContext.Builder().build()

    RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(applicationContext.env))
        .build()
        .apply { registerApplicationContext(applicationContext) }
        .start()
}