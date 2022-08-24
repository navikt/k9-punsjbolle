package no.nav.punsjbolle.sak

import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPost
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.jsonBody
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.punsjbolle.AktørId
import no.nav.punsjbolle.AzureAwareClient
import no.nav.punsjbolle.CorrelationId
import no.nav.punsjbolle.K9Saksnummer
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.net.URI

internal class SakClient(
    baseUrl: URI,
    accessTokenClient: AccessTokenClient,
    scopes: Set<String>
) : AzureAwareClient(
    navn = "SakClient",
    accessTokenClient = accessTokenClient,
    scopes = scopes,
    pingUrl = URI("$baseUrl/isAlive")
) {

    private val OpprettSakUrl = URI("$baseUrl/api/v1/saker")

    internal suspend fun forsikreSakskoblingFinnes(
        saksnummer: K9Saksnummer,
        søker: AktørId,
        correlationId: CorrelationId
    ) {

        @Language("JSON")
        val dto = """
            {
              "tema": "OMS",
              "applikasjon": "K9",
              "aktoerId": "$søker",
              "fagsakNr": "$saksnummer"
            }
        """.trimIndent()

        val (httpStatusCode, response) = OpprettSakUrl.toString().httpPost {
            it.header(HttpHeaders.Authorization, authorizationHeader())
            it.header(HttpHeaders.XCorrelationId, "$correlationId")
            it.accept(ContentType.Application.Json)
            it.jsonBody(dto)
        }.readTextOrThrow()

        when (httpStatusCode) {
            HttpStatusCode.Created -> logger.info("Opprettet sakskobling.")
            HttpStatusCode.Conflict -> logger.info("Sakskobling finnes allerede.")
            else -> throw IllegalStateException(
                "Feil fra Sak. URL=[$OpprettSakUrl], HttpStatusCode=[${httpStatusCode.value}], Response=[$response]"
            )
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(SakClient::class.java)
    }
}