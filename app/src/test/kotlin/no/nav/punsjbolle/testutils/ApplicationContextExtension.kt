package no.nav.punsjbolle.testutils

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.punsjbolle.ApplicationContext
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import javax.sql.DataSource

internal class ApplicationContextExtension : ParameterResolver {
    internal companion object {

        internal fun ApplicationContext.Builder.buildStarted() = build().also { it.start() }

        private val mockedEnvironment = MockedEnvironment().start()

        private val env = mapOf(
            "DATABASE_HOST" to "localhost",
            "DATABASE_PORT" to "${mockedEnvironment.embeddedPostgres.port}",
            "DATABASE_DATABASE" to "postgres",
            "DATABASE_USERNAME" to "postgres",
            "DATABASE_PASSWORD" to "postgres"
        )

        init {
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    mockedEnvironment.stop()
                }
            )
        }
    }

    private val støttedeParametre = listOf(
        ApplicationContext.Builder::class.java,
        WireMockServer::class.java
    )

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return støttedeParametre.contains(parameterContext.parameter.type)
    }

    private fun DataSource.cleanAndMigrate() = this.also {
        Flyway
            .configure()
            .dataSource(this)
            .load()
            .also {
                it.clean()
                it.migrate()
            }
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return when (parameterContext.parameter.type) {
            WireMockServer::class.java -> mockedEnvironment.wireMockServer
            else -> ApplicationContext.Builder(
                env = env,
                onStart = {
                    it.dataSource.cleanAndMigrate()
                }
            )
        }
    }
}
