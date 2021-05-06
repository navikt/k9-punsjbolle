package no.nav.punsjbolle.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

private const val path = "/k9-sak-mock"

private fun WireMockServer.mockPingUrl(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/internal/health/isReady"))
            .willReturn(WireMock.aResponse().withStatus(200)))
    return this
}

// TODO: Utbedre mock
private fun WireMockServer.mockHentSaksnummer(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$path/fordel/fagsak/opprett"))
            .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("""
                {"saksnummer": "SAK123"}
            """.trimIndent())))
    return this
}

// TODO: Utbedre mock
private fun WireMockServer.mockSendInnSøknad(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$path/fordel/journalposter"))
            .willReturn(WireMock.aResponse().withStatus(204)))
    return this
}

internal fun WireMockServer.mockK9Sak() = mockPingUrl().mockHentSaksnummer().mockSendInnSøknad()
internal fun WireMockServer.k9SakBaseUrl() = baseUrl() + path
