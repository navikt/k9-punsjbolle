package no.nav.punsjbolle

import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient

import no.nav.k9.rapid.river.Environment
import no.nav.k9.rapid.river.RapidsStateListener
import no.nav.k9.rapid.river.csvTilSet
import no.nav.k9.rapid.river.hentRequiredEnv
import no.nav.punsjbolle.infotrygd.InfotrygdClient
import no.nav.punsjbolle.joark.SafClient
import no.nav.punsjbolle.journalpost.PunsjbarJournalpostClient
import no.nav.punsjbolle.k9sak.K9SakClient
import no.nav.punsjbolle.ruting.RutingService
import no.nav.punsjbolle.sak.SakClient
import java.net.URI

internal class ApplicationContext(
    internal val env: Environment,
    internal val healthChecks: Set<HealthCheck>,
    internal val accessTokenClient: AccessTokenClient,
    internal val k9SakClient: K9SakClient,
    internal val sakClient: SakClient,
    internal val safClient: SafClient,
    internal val infotrygdClient: InfotrygdClient,
    internal val punsjbarJournalpostClient: PunsjbarJournalpostClient,
    internal val rutingService: RutingService,
    private val onStart: (applicationContext: ApplicationContext) -> Unit,
    private val onStop: (applicationContext: ApplicationContext) -> Unit) {

    internal fun start() = onStart(this)
    internal fun stop() = onStop(this)
    internal var rapidsState = RapidsStateListener.RapidsState.initialState()

    internal class Builder(
        var env: Environment? = null,
        var accessTokenClient: AccessTokenClient? = null,
        var k9SakClient: K9SakClient? = null,
        var sakClient: SakClient? = null,
        var safClient: SafClient? = null,
        var infotrygdClient: InfotrygdClient? = null,
        var punsjbarJournalpostClient: PunsjbarJournalpostClient? = null,
        var rutingService: RutingService? = null,
        var onStart: (applicationContext: ApplicationContext) -> Unit = {},
        var onStop: (applicationContext: ApplicationContext) -> Unit = {}) {

        internal fun build(): ApplicationContext {
            val benyttetEnv = env ?: System.getenv()

            val benyttetAccessTokenClient = accessTokenClient ?: ClientSecretAccessTokenClient(
                clientId = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
                clientSecret = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_SECRET"),
                tokenEndpoint = URI(benyttetEnv.hentRequiredEnv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"))
            )

            val benyttetK9SakClient = k9SakClient ?: K9SakClient(
                baseUrl = URI(benyttetEnv.hentRequiredEnv("K9_SAK_BASE_URL")),
                accessTokenClient = benyttetAccessTokenClient,
                scopes = benyttetEnv.hentRequiredEnv("K9_SAK_SCOPES").csvTilSet(),
            )

            val benyttetSakClient = sakClient ?: SakClient(
                baseUrl = URI(benyttetEnv.hentRequiredEnv("SAK_BASE_URL")),
                accessTokenClient = benyttetAccessTokenClient,
                scopes = benyttetEnv.hentRequiredEnv("SAK_SCOPES").csvTilSet()
            )

            val benyttetSafClient = safClient ?: SafClient(
                baseUrl = URI(benyttetEnv.hentRequiredEnv("SAF_BASE_URL")),
                accessTokenClient = benyttetAccessTokenClient,
                scopes = benyttetEnv.hentRequiredEnv("SAF_SCOPES").csvTilSet(),
            )

            val benyttetInfotrygdClient = infotrygdClient ?: InfotrygdClient(
                baseUrl = URI(benyttetEnv.hentRequiredEnv("INFOTRYGD_GRUNNLAG_PAAROERENDE_SYKDOM_BASE_URL")),
                accessTokenClient = benyttetAccessTokenClient,
                scopes = benyttetEnv.hentRequiredEnv("INFOTRYGD_GRUNNLAG_PAAROERENDE_SYKDOM_SCOPES").csvTilSet()
            )

            val benyttetPunsjbarJournalpostClient = punsjbarJournalpostClient ?: PunsjbarJournalpostClient()

            return ApplicationContext(
                env = benyttetEnv,
                accessTokenClient = benyttetAccessTokenClient,
                k9SakClient = benyttetK9SakClient,
                safClient = benyttetSafClient,
                infotrygdClient = benyttetInfotrygdClient,
                healthChecks = setOf(benyttetK9SakClient, benyttetSafClient, benyttetInfotrygdClient, benyttetSakClient),
                onStart = onStart,
                onStop = onStop,
                rutingService = rutingService ?: RutingService(
                    k9SakClient = benyttetK9SakClient,
                    infotrygdClient = benyttetInfotrygdClient
                ),
                sakClient = benyttetSakClient,
                punsjbarJournalpostClient = benyttetPunsjbarJournalpostClient
            )
        }
    }
}
