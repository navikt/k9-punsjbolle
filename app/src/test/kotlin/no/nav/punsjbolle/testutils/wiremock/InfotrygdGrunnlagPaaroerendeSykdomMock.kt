package no.nav.punsjbolle.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.Body
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withAuthorizationHeader
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withNavGetHeaders
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withJson
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withNavPostHeaders

private const val path = "/infotrygd-grunnlag-paaroerende-sykdom-mock"

private fun WireMockServer.mockSaker(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$path/saker.*"))
            .withNavPostHeaders().withRequestBody(AnythingPattern())
            .willReturn(WireMock.aResponse().withJson("""[{"saker":[], "vedtak":[]}]""")))
    return this
}

private fun WireMockServer.mockVedtakForPleietrengende(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$path/vedtakForPleietrengende.*"))
            .withNavPostHeaders().withRequestBody(AnythingPattern())
            .willReturn(WireMock.aResponse().withJson("""[{"vedtak":[]}]""")))
    return this
}

internal fun WireMockServer.mockInfotrygdGrunnlagPaaroerendeSykdom() = mockSaker().mockVedtakForPleietrengende()
internal fun WireMockServer.infotrygdGrunnlagPaaroerendeSykdomBaseUrl() = baseUrl() + path