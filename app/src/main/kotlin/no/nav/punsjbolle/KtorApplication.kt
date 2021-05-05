package no.nav.punsjbolle

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.routing.*
import no.nav.helse.dusseldorf.ktor.health.*

internal fun Application.punsjbolle(
    applicationContext: ApplicationContext = ApplicationContext.Builder().build()) {

    install(ContentNegotiation) {
        jackson {}
    }

    val healthService = HealthService(
        healthChecks = applicationContext.healthChecks.plus(object : HealthCheck {
            override suspend fun check() : Result {
                val currentState = applicationContext.rapidsState
                return when (currentState.isHealthy()) {
                    true -> Healthy("RapidsConnection", currentState.asMap)
                    false -> UnHealthy("RapidsConnection", currentState.asMap)
                }
            }
        })
    )

    HealthReporter(
        app = "k9-punsjbolle",
        healthService = healthService
    )

    routing {
        HealthRoute(healthService = healthService)
    }
}

