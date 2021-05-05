package no.nav.punsjbolle.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

private const val path = "/saf-mock"

private fun WireMockServer.mockPingUrl(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/isReady"))
            .willReturn(WireMock.aResponse().withStatus(200)))
    return this
}

internal fun WireMockServer.mockSaf() = mockPingUrl()
internal fun WireMockServer.safBaseUrl() = baseUrl() + path
