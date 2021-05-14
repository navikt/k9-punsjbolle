package no.nav.punsjbolle.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withAuthorizationHeader
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withDefaultPostHeaders
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withJson
import org.intellij.lang.annotations.Language

private const val path = "/saf-mock"
@Language("JSON")
private val hentJounralpostResponse = """
    {
      "data": {
        "journalpost": {
          "journalposttype": "I",
          "journalpoststatus": "",
          "datoOpprettet": "2018-01-01T12:00:00",
          "journalstatus": "MOTTATT",
          "sak": null,
          "dokumenter": [{"brevkode": "NAV-123"}]
        }
      }
    }
""".trimIndent()

private fun WireMockServer.mockPingUrl(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/isReady")).withAuthorizationHeader()
            .willReturn(WireMock.aResponse().withStatus(200)))
    return this
}

private fun WireMockServer.mockHentJournalpost(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$path/graphql")).withDefaultPostHeaders()
            .willReturn(WireMock.aResponse().withJson(hentJounralpostResponse))
    )
    return this
}

internal fun WireMockServer.mockSaf() = mockPingUrl().mockHentJournalpost()
internal fun WireMockServer.safBaseUrl() = baseUrl() + path
