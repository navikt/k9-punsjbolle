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

internal fun WireMockServer.mockK9Sak() = mockPingUrl()
internal fun WireMockServer.k9SakBaseUrl() = baseUrl() + path
