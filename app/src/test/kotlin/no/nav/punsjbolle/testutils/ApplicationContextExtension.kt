package no.nav.punsjbolle.testutils

import com.github.tomakehurst.wiremock.WireMockServer
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2JwksUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2TokenUrl
import no.nav.punsjbolle.ApplicationContext
import no.nav.punsjbolle.journalpost.PunsjbarJournalpostClient
import no.nav.punsjbolle.testutils.wiremock.k9SakBaseUrl
import no.nav.punsjbolle.testutils.wiremock.safBaseUrl
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

internal class ApplicationContextExtension : ParameterResolver {
    internal companion object {

        private val mockedEnvironment = MockedEnvironment().start()

        private val env = mapOf(
            "AZURE_APP_CLIENT_ID" to "k9-punsjbolle",
            "AZURE_APP_CLIENT_SECRET" to "foo",
            "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to mockedEnvironment.wireMockServer.getAzureV2TokenUrl(),
            "AZURE_OPENID_CONFIG_ISSUER" to Azure.V2_0.getIssuer(),
            "AZURE_OPENID_CONFIG_JWKS_URI" to mockedEnvironment.wireMockServer.getAzureV2JwksUrl(),
            "K9_SAK_BASE_URL" to mockedEnvironment.wireMockServer.k9SakBaseUrl(),
            "K9_SAK_SCOPES" to "k9-sak/.default",
            "SAF_BASE_URL" to mockedEnvironment.wireMockServer.safBaseUrl(),
            "SAF_SCOPES" to "saf/.default",
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

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return when (parameterContext.parameter.type) {
            WireMockServer::class.java -> mockedEnvironment.wireMockServer
            else -> ApplicationContext.Builder(
                env = env,
                punsjbarJournalpostClient = mockk<PunsjbarJournalpostClient>().also {
                    every { it.send(any()) }.returns(Unit)
                    every { it.close() }.returns(Unit)
                }
            )
        }
    }
}
