package no.nav.punsjbolle

import no.nav.helse.dusseldorf.ktor.health.HealthCheck

import no.nav.k9.rapid.river.Environment
import no.nav.k9.rapid.river.RapidsStateListener

import javax.sql.DataSource

internal class ApplicationContext(
    internal val env: Environment,
    internal val dataSource: DataSource,
    internal val healthChecks: Set<HealthCheck>,
    private val onStart: (applicationContext: ApplicationContext) -> Unit,
    private val onStop: (applicationContext: ApplicationContext) -> Unit) {

    internal fun start() = onStart(this)
    internal fun stop() = onStop(this)
    internal var rapidsState = RapidsStateListener.RapidsState.initialState()

    internal class Builder(
        var env: Environment? = null,
        var dataSource: DataSource? = null,
        var onStart: (applicationContext: ApplicationContext) -> Unit = {
            it.dataSource.migrate()
        },
        var onStop: (applicationContext: ApplicationContext) -> Unit = {}) {
        internal fun build(): ApplicationContext {
            val benyttetEnv = env ?: System.getenv()
            val benyttetDataSource = dataSource ?: DataSourceBuilder(benyttetEnv).build()

            return ApplicationContext(
                env = benyttetEnv,
                dataSource = benyttetDataSource,
                healthChecks = setOf(),
                onStart = onStart,
                onStop = onStop
            )
        }
    }
}
