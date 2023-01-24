package no.nav.punsjbolle.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withAuthorizationHeader
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withNavPostHeaders
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withJson

private const val path = "/k9-sak-mock"

private fun WireMockServer.mockPingUrl(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/internal/health/isReady")).withAuthorizationHeader()
            .willReturn(WireMock.aResponse().withStatus(200)))
    return this
}

private fun WireMockServer.mockHentSaksnummer(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$path/api/fordel/fagsak/opprett"))
            .withNavPostHeaders()
            .withRequestBody(WireMock.matchingJsonPath("$.ytelseType"))
            .withRequestBody(WireMock.matchingJsonPath("$.aktørId"))
            //.withRequestBody(WireMock.matchingJsonPath("$.pleietrengendeAktørId"))
            .withRequestBody(WireMock.matchingJsonPath("$.periode"))
            .willReturn(WireMock.aResponse().withJson("""{"saksnummer": "SAK123"}""")))
    return this
}

private fun WireMockServer.mockSendInnSøknad(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$path/api/fordel/journalposter"))
            .withNavPostHeaders()
            .withRequestBody(WireMock.matchingJsonPath("$.[0].saksnummer"))
            .withRequestBody(WireMock.matchingJsonPath("$.[0].journalpostId"))
            .withRequestBody(WireMock.matchingJsonPath("$.[0].ytelseType.kode"))
            .withRequestBody(WireMock.matchingJsonPath("$.[0].ytelseType.kodeverk", WireMock.equalTo("FAGSAK_YTELSE")))
            .withRequestBody(WireMock.matchingJsonPath("$.[0].kanalReferanse"))
            .withRequestBody(WireMock.matchingJsonPath("$.[0].type"))
            .withRequestBody(WireMock.matchingJsonPath("$.[0].forsendelseMottattTidspunkt"))
            .withRequestBody(WireMock.matchingJsonPath("$.[0].forsendelseMottatt"))
            .withRequestBody(WireMock.matchingJsonPath("$.[0].payload"))
            .willReturn(WireMock.aResponse().withStatus(204)))
    return this
}

private fun WireMockServer.mockPleiepengerSyktBarnUnntaksliste(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$path/api/fordel/psb-infotrygd/finnes"))
            .withNavPostHeaders()
            .withRequestBody(WireMock.matchingJsonPath("$.aktører"))
            .willReturn(WireMock.aResponse().withStatus(200).withBody("false")))
    return this
}


private fun WireMockServer.mockMatchFagsak(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$path/api/fagsak/match"))
            .withNavPostHeaders()
            .withRequestBody(WireMock.matchingJsonPath("$.ytelseType.kode"))
            .withRequestBody(WireMock.matchingJsonPath("$.ytelseType.kodeverk", WireMock.equalTo("FAGSAK_YTELSE")))
            .willReturn(WireMock.aResponse().withJson("[]")))
    return this
}


internal fun WireMockServer.mockK9Sak() = mockPingUrl().mockHentSaksnummer().mockSendInnSøknad().mockMatchFagsak().mockPleiepengerSyktBarnUnntaksliste()
internal fun WireMockServer.k9SakBaseUrl() = baseUrl() + path
