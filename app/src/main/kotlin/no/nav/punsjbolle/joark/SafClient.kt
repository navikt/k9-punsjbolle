package no.nav.punsjbolle.joark

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpHeaders.Authorization
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPost
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.jsonBody
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.punsjbolle.AzureAwareClient
import no.nav.punsjbolle.CorrelationId
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.Json.objectOrEmptyObject
import no.nav.punsjbolle.Json.stringOrNull
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import java.net.URI
import java.time.LocalDateTime

internal class SafClient(
    baseUrl: URI,
    accessTokenClient: AccessTokenClient,
    scopes: Set<String>
) : AzureAwareClient(
    navn = "SafClient",
    accessTokenClient = accessTokenClient,
    scopes = scopes
) {

    private val GraphQlEndpoint = "$baseUrl/graphql"

    internal suspend fun hentJournalpost(
        journalpostId: JournalpostId,
        correlationId: CorrelationId
    ): Journalpost {

        val (httpStatusCode, responseBody) = GraphQlEndpoint.httpPost {
            it.header(Authorization, authorizationHeader())
            it.header(CorrelationIdHeaderKey, "$correlationId")
            it.header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            it.accept(ContentType.Application.Json)
            it.jsonBody(hentJournalpostGraphQlRequestBody(journalpostId))
        }.readTextOrThrow()

        require(httpStatusCode.isSuccess()) {
            "Feil fra SAF. URL=[$GraphQlEndpoint], HttpStatusCode=[${httpStatusCode.value}], Response=[$responseBody], JournalpostId=[$journalpostId]"
        }

        return kotlin.runCatching {
            JSONObject(responseBody)
                .getJSONObject("data")
                .getJSONObject("journalpost")
                .somJournalpost(journalpostId)
        }.fold(onSuccess = { it }, onFailure = { throwable ->
            throw IllegalStateException(
                "Feil ved mapping av response fra SAF. Response=[$responseBody], JournalpostId=[$journalpostId]",
                throwable
            )
        })
    }


    internal companion object {
        private val logger = LoggerFactory.getLogger(SafClient::class.java)

        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsjbolle"
        private const val CorrelationIdHeaderKey = "Nav-Callid"

        private fun hentJournalpostGraphQlRequestBody(journalpostId: JournalpostId) = """
        query {
            journalpost(journalpostId: "$journalpostId") {
                journalposttype,
                journalstatus,
                datoOpprettet,
                eksternReferanseId,
                sak {
                    fagsakId,
                    fagsaksystem
                },
                dokumenter {
                    brevkode
                }
            }
        }
        """.trimIndent().replace("\n", "").replace("  ", "").let { query ->
            JSONObject().apply {
                put("query", query)
            }.toString()
        }


        internal fun JSONObject.somJournalpost(journalpostId: JournalpostId) = Journalpost(
            journalpostId = journalpostId,
            journalposttype = getString("journalposttype"),
            journalpoststatus = getString("journalstatus"),
            eksternReferanse = stringOrNull("eksternReferanseId"),
            brevkode = getJSONArray("dokumenter").mapNotNull { (it as JSONObject).stringOrNull("brevkode") }
                .firstOrNull(),
            opprettet = LocalDateTime.parse(getString("datoOpprettet")),
            sak = objectOrEmptyObject("sak")?.let { sak ->
                val fagsakId = sak.stringOrNull("fagsakId")
                val fagsaksystem = sak.stringOrNull("fagsaksystem")
                when {
                    fagsakId == null && fagsaksystem == null -> null
                    fagsakId != null && fagsaksystem != null -> Journalpost.Sak(
                        fagsakId = fagsakId,
                        fagsaksystem = fagsaksystem
                    )
                    else -> logger.warn("Journalpost har kun en av Fagsaksystem/FagsakId satt. SafResponse=$this")
                        .let { null }
                }
            }
        )
    }
}