package no.nav.punsjbolle

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.k9.rapid.river.RapidsStateListener
import no.nav.punsjbolle.journalpost.KopierPunsjbarJournalpostSteg1River
import no.nav.punsjbolle.journalpost.KopierPunsjbarJournalpostSteg2River
import no.nav.punsjbolle.journalpost.KopierPunsjbarJournalpostSteg3River

internal fun RapidsConnection.registerApplicationContext(applicationContext: ApplicationContext) {
    KopierPunsjbarJournalpostSteg1River(
        rapidsConnection = this
    )
    KopierPunsjbarJournalpostSteg2River(
        rapidsConnection = this,
        k9SakClient = applicationContext.k9SakClient,
        safClient = applicationContext.safClient
    )
    KopierPunsjbarJournalpostSteg3River(
        rapidsConnection = this,
        punsjbarJournalpostClient = applicationContext.punsjbarJournalpostClient
    )

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