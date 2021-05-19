package no.nav.punsjbolle.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withAuthorizationHeader
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withDefaultGetHeaders
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withJson

private const val path = "/infotrygd-grunnlag-paaroerende-sykdom-mock"

private fun WireMockServer.mockPingUrl(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/actuator/health")).withAuthorizationHeader()
            .willReturn(WireMock.aResponse().withStatus(200)))
    return this
}

private fun WireMockServer.mockSaker(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/saker.*"))
            .withDefaultGetHeaders().withQueryParam("fnr", AnythingPattern()).withQueryParam("fom", AnythingPattern())
            .willReturn(WireMock.aResponse().withJson("""{"saker":[], "vedtak":[]}""")))
    return this
}

private fun WireMockServer.mockVedtakForPleietrengende(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/vedtakForPleietrengende.*"))
            .withDefaultGetHeaders().withQueryParam("fnr", AnythingPattern()).withQueryParam("fom", AnythingPattern())
            .willReturn(WireMock.aResponse().withJson("""{"vedtak":[]}""")))
    return this
}

internal fun WireMockServer.mockInfotrygdGrunnlagPaaroerendeSykdom() = mockPingUrl().mockSaker().mockVedtakForPleietrengende()
internal fun WireMockServer.infotrygdGrunnlagPaaroerendeSykdomBaseUrl() = baseUrl() + path