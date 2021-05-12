package no.nav.punsjbolle.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

private const val path = "/infotrygd-grunnlag-paaroerende-sykdom-mock"

private fun WireMockServer.mockPingUrl(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/isReady"))
            .willReturn(WireMock.aResponse().withStatus(200)))
    return this
}

internal fun WireMockServer.mockInfotrygdGrunnlagPaaroerendeSykdom() = mockPingUrl()
internal fun WireMockServer.infotrygdGrunnlagPaaroerendeSykdomBaseUrl() = baseUrl() + path