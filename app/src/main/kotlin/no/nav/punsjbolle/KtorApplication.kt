package no.nav.punsjbolle

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.routing.*
import no.nav.helse.dusseldorf.ktor.auth.*
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.helse.dusseldorf.ktor.health.*
import no.nav.k9.rapid.river.hentRequiredEnv
import no.nav.punsjbolle.api.SaksnummerApi
import java.net.URI

internal fun Application.punsjbolle(
    applicationContext: ApplicationContext = ApplicationContext.Builder().build()) {

    install(ContentNegotiation) {
        jackson {}
    }
    install(StatusPages) {
        AuthStatusPages()
        DefaultStatusPages()
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

    val azureV2 = Issuer(
        issuer = applicationContext.env.hentRequiredEnv("AZURE_OPENID_CONFIG_ISSUER"),
        jwksUri = URI(applicationContext.env.hentRequiredEnv("AZURE_OPENID_CONFIG_JWKS_URI")),
        audience = applicationContext.env.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
        alias = "azure-v2"
    )

    val issuers = mapOf(
        azureV2.alias() to azureV2,
    ).withoutAdditionalClaimRules()

    install(Authentication) {
        multipleJwtIssuers(
            issuers = issuers
        )
    }

    HealthReporter(
        app = "k9-punsjbolle",
        healthService = healthService
    )

    preStopOnApplicationStopPreparing(preStopActions = listOf(
        FullførAktiveRequester(this)
    ))

    install(CallId) {
        fromXCorrelationIdHeader()
    }

    install(CallLogging) {
        logRequests()
        correlationIdAndRequestIdInMdc()
    }

    routing {
        HealthRoute(healthService = healthService)
        authenticate(*issuers.allIssuers()) {
            route("/api") {
                SaksnummerApi(
                    rutingService = applicationContext.rutingService,
                    safClient = applicationContext.safClient,
                    k9SakClient = applicationContext.k9SakClient
                )
            }
        }
    }
}

