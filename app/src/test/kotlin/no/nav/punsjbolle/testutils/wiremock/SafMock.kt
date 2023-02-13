package no.nav.punsjbolle.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withAuthorizationHeader
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withNavPostHeaders
import no.nav.punsjbolle.testutils.wiremock.WireMockVerktøy.withJson
import org.intellij.lang.annotations.Language

private const val path = "/saf-mock"
@Language("JSON")
private val hentJounralpostResponse = """
    {
      "data": {
        "journalpost": {
          "journalposttype": "I",
          "journalstatus": "MOTTATT",
          "datoOpprettet": "2018-01-01T12:00:00",
          "sak": null,
          "dokumenter": [{"brevkode": "NAV-123"}]
        }
      }
    }
""".trimIndent()

private fun WireMockServer.mockHentJournalpost(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$path/graphql")).withNavPostHeaders()
            .willReturn(WireMock.aResponse().withJson(hentJounralpostResponse))
    )
    return this
}

internal fun WireMockServer.mockSaf() = mockHentJournalpost()
internal fun WireMockServer.safBaseUrl() = baseUrl() + path
