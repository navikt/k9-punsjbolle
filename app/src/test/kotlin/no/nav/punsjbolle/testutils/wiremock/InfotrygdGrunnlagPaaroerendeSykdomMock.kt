package no.nav.punsjbolle.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

private const val path = "/infotrygd-grunnlag-paaroerende-sykdom-mock"

private fun WireMockServer.mockPingUrl(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/actuator/health"))
            .willReturn(WireMock.aResponse().withStatus(200)))
    return this
}

// TODO: Utbedre mock
private fun WireMockServer.mockSaker(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/saker.*"))
            .willReturn(WireMock.aResponse().withHeader("Content-Type", "application/json").withBody("""
                {"saker":[], "vedtak":[]}
            """.trimIndent()).withStatus(200)))
    return this
}

// TODO: Utbedre mock
private fun WireMockServer.mockVedtakForPleietrengende(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/vedtakForPleietrengende.*"))
            .willReturn(WireMock.aResponse().withHeader("Content-Type", "application/json").withBody("""
                {"vedtak":[]}
            """.trimIndent()).withStatus(200)))
    return this
}

internal fun WireMockServer.mockInfotrygdGrunnlagPaaroerendeSykdom() = mockPingUrl().mockSaker().mockVedtakForPleietrengende()
internal fun WireMockServer.infotrygdGrunnlagPaaroerendeSykdomBaseUrl() = baseUrl() + path