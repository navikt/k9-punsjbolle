package no.nav.punsjbolle

import com.nimbusds.jwt.SignedJWT
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient

internal abstract class AzureAwareClient(
    private val navn: String,
    private val accessTokenClient: AccessTokenClient,
    private val scopes: Set<String>,
    private val requireAccessAsAppliation: Boolean = true
) : HealthCheck {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    protected fun authorizationHeader() =
        cachedAccessTokenClient.getAccessToken(scopes).asAuthoriationHeader()

    override suspend fun check() =
        Result.merge(
            navn,
            accessTokenCheck(),
        )

    private fun accessTokenCheck() = kotlin.runCatching {
        val accessTokenResponse = accessTokenClient.getAccessToken(scopes)
        when (requireAccessAsAppliation) {
            true -> (SignedJWT.parse(accessTokenResponse.accessToken).jwtClaimsSet.getStringArrayClaim("roles")
                ?.toList()
                ?: emptyList()).contains("access_as_application")
            false -> true
        }
    }.fold(
        onSuccess = {
            when (it) {
                true -> Healthy("AccessTokenCheck", "OK")
                false -> UnHealthy("AccessTokenCheck", "Feil: Mangler rettigheter")
            }
        },
        onFailure = { UnHealthy("AccessTokenCheck", "Feil: ${it.message}") }
    )
}