package no.nav.punsjbolle.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withAuthorizationHeader
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withDefaultPostHeaders

private const val path = "/sak-mock"

private fun WireMockServer.mockOpprettSak(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$path/api/v1/saker"))
            .withDefaultPostHeaders()
            .withRequestBody(WireMock.matchingJsonPath("$.tema",WireMock.equalTo("OMS")))
            .withRequestBody(WireMock.matchingJsonPath("$.applikasjon", WireMock.equalTo("K9")))
            .withRequestBody(WireMock.matchingJsonPath("$.aktoerId"))
            .withRequestBody(WireMock.matchingJsonPath("$.fagsakNr"))
            .willReturn(WireMock.aResponse().withStatus(when ((0..1).random()) {
                0 -> 201
                else -> 409
            })))
    return this
}


internal fun WireMockServer.mockSak() = mockOpprettSak()
internal fun WireMockServer.sakBaseUrl() = baseUrl() + path
