package no.nav.punsjbolle.joark

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpHeaders.Authorization
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPost
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.jsonBody
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.punsjbolle.AzureAwareClient
import no.nav.punsjbolle.CorrelationId
import no.nav.punsjbolle.JournalpostId
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
    scopes = scopes,
    pingUrl = URI("$baseUrl/isReady")) { // TODO: Fix

    private val GraphQlEndpoint = "$baseUrl/graphql"

    internal suspend fun hentJournalposter(
        journalpostIder: Set<JournalpostId>,
        correlationId: CorrelationId): Set<Journalpost> {
        logger.info("Henter journalposter for JounralpostIder=$journalpostIder")

        return coroutineScope {
            val deferred = mutableSetOf<Deferred<Journalpost>>()
            journalpostIder.forEach { journalpostId ->
                deferred.add(async {
                    hentJournalpost(
                        journalpostId = journalpostId,
                        correlationId = correlationId
                    )
                })
            }
            deferred.awaitAll().toSet()
        }
    }


    private suspend fun hentJournalpost(
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
            throw IllegalStateException("Feil ved mapping av response fra SAF. Response=[$responseBody], JournalpostId=[$journalpostId]", throwable)
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
                    fagsakId
                    fagsaksystem,
                },
                dokumenter {
                    brevkode
                }
            }
        """.trimIndent().replace("\n", "").replace("  ", "").let { query ->
            JSONObject().apply {
                put("query", query)
            }.toString()
        }

        private fun JSONObject.getStringOrNull(key: String) = when (has(key) && get(key) is String) {
            true -> getString(key)
            else -> null
        }

        private fun JSONObject.getJSONObjectOrNull(key: String) = when (has(key) && get(key) is JSONObject) {
            true -> getJSONObject(key)
            else -> null
        }

        internal fun JSONObject.somJournalpost(journalpostId: JournalpostId) = Journalpost(
            journalpostId = journalpostId,
            journalposttype = getString("journalposttype"),
            journalpoststatus = getString("journalstatus"),
            eksternReferanse = getStringOrNull("eksternReferanseId"),
            brevkode = getJSONArray("dokumenter").mapNotNull { (it as JSONObject).getStringOrNull("brevkode") }.firstOrNull(),
            opprettet = LocalDateTime.parse(getString("datoOpprettet")),
            sak = getJSONObjectOrNull("sak")?.let { sak -> Journalpost.Sak(
                fagsakId = sak.getString("fagsakId"),
                fagsaksystem = sak.getString("fagsaksystem")
            )}
        )
    }
}