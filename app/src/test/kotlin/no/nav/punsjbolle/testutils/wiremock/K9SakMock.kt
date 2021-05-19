package no.nav.punsjbolle.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withAuthorizationHeader
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withDefaultPostHeaders
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withJson

private const val path = "/k9-sak-mock"

private fun WireMockServer.mockPingUrl(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/internal/health/isReady")).withAuthorizationHeader()
            .willReturn(WireMock.aResponse().withStatus(200)))
    return this
}

// TODO: Utbedre mock
private fun WireMockServer.mockHentSaksnummer(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$path/api/fordel/fagsak/opprett")).withDefaultPostHeaders()
            .willReturn(WireMock.aResponse().withJson("""{"saksnummer": "SAK123"}""")))
    return this
}

// TODO: Utbedre mock
private fun WireMockServer.mockSendInnSøknad(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$path/api/fordel/journalposter")).withDefaultPostHeaders()
            .willReturn(WireMock.aResponse().withStatus(204)))
    return this
}

// TODO: Utbedre mock
private fun WireMockServer.mockMatchFagsak(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$path/api/fagsak/match")).withDefaultPostHeaders()
            .willReturn(WireMock.aResponse().withJson("[]")))
    return this
}


internal fun WireMockServer.mockK9Sak() = mockPingUrl().mockHentSaksnummer().mockSendInnSøknad().mockMatchFagsak()
internal fun WireMockServer.k9SakBaseUrl() = baseUrl() + path
