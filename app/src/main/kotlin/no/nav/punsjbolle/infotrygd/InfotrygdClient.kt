package no.nav.punsjbolle.infotrygd

import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpGet
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.punsjbolle.AzureAwareClient
import no.nav.punsjbolle.CorrelationId
import no.nav.punsjbolle.Identitetsnummer
import no.nav.punsjbolle.ruting.RutingGrunnlag
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate

internal class InfotrygdClient(
    baseUrl: URI,
    accessTokenClient: AccessTokenClient,
    scopes: Set<String>
) : AzureAwareClient(
    navn = "InfotrygdClient",
    accessTokenClient = accessTokenClient,
    scopes = scopes,
    pingUrl = URI("$baseUrl/actuator/health")) {

    private val HentSakerUrl = URI("$baseUrl/saker")
    private val HentVedtakForPleietrengende = URI("$baseUrl/vedtakForPleietrengende")

    internal suspend fun harLøpendeSakSomInvolvererEnAv(
        fraOgMed: LocalDate,
        søker: Identitetsnummer,
        pleietrengende: Identitetsnummer?,
        annenPart: Identitetsnummer?,
        correlationId: CorrelationId
    ) = RutingGrunnlag(
        søker = harSakSomSøker(søker, fraOgMed, correlationId),
        pleietrengende = pleietrengende?.let { harSakSomPleietrengende(it, fraOgMed, correlationId) } ?: false,
        annenPart = annenPart?.let { harSakSomSøker(it, fraOgMed, correlationId) } ?: false
    )

    private suspend fun harSakSomSøker(
        identitetsnummer: Identitetsnummer,
        fraOgMed: LocalDate,
        correlationId: CorrelationId) : Boolean {
        val url = URI("$HentSakerUrl?fnr=$identitetsnummer&fom=$fraOgMed")

        val (httpStatusCode, response) = url.httpGet {
            it.header(HttpHeaders.Authorization, authorizationHeader())
            it.header(CorrelationIdHeaderKey, "$correlationId")
            it.header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            it.accept(ContentType.Application.Json)
        }.readTextOrThrow()

        require(httpStatusCode.isSuccess()) {
            "Feil fra Infotrygd. URL=[$HentSakerUrl], HttpStatusCode=[${httpStatusCode.value}], Response=[$response]"
        }

        val json = JSONObject(response)
        val saker = json.innehoderAktuellSak("saker")
        val vedtak = json.innehoderAktuellSak("vedtak")
        return (saker || vedtak).also { if (it) {
            secureLogger.info("Fant sak(er) i Infotrygd som søker for Identitetsnummer=[$identitetsnummer], FraOgMed=[$fraOgMed], Response=[$response]")
        }}
    }

    private suspend fun harSakSomPleietrengende(
        identitetsnummer: Identitetsnummer,
        fraOgMed: LocalDate,
        correlationId: CorrelationId) : Boolean {

        val url = URI("$HentVedtakForPleietrengende?fnr=$identitetsnummer&fom=$fraOgMed")

        val (httpStatusCode, response) = url.httpGet {
            it.header(HttpHeaders.Authorization, authorizationHeader())
            it.header(CorrelationIdHeaderKey, "$correlationId")
            it.header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            it.accept(ContentType.Application.Json)
        }.readTextOrThrow()

        require(httpStatusCode.isSuccess()) {
            "Feil fra Infotrygd. URL=[$HentVedtakForPleietrengende], HttpStatusCode=[${httpStatusCode.value}], Response=[$response]"
        }

        val json = JSONObject(response)
        return json.innehoderAktuellSak("vedtak").also { if (it) {
            secureLogger.info("Fant sak(er) i Infotrygd som pleietrengende for Identitetsnummer=[$identitetsnummer], FraOgMed=[$fraOgMed], Response=[$response]")
        }}
    }

    private companion object {
        private val secureLogger = LoggerFactory.getLogger("tjenestekall")
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsjbolle"
        private const val CorrelationIdHeaderKey = "Nav-Callid"
        private val relevanteBehandlingstema = listOf(
            "OM", "OP", "PB", "PP", "PN"
        )

        private fun JSONObject.getJSONArrayOrEmptyArray(key: String) = when (has(key) && get(key) is JSONArray) {
            true -> getJSONArray(key)
            false -> JSONArray()
        }

        private fun JSONObject.hasString(key: String) = has(key) && get(key) is String

        private fun JSONObject.innehoderAktuellSak(key: String) =
            getJSONArrayOrEmptyArray(key)
            .asSequence()
            .map { it as JSONObject }
            .filter { it.hasString("tema") }
            .filter { it.getString("tema").toUpperCase() == "BS" }
            .filter { it.hasString("behandlingstema") }
            .filter { relevanteBehandlingstema.contains(it.getString("behandlingstema").toUpperCase()) }
            .toList()
            .isNotEmpty()
    }
}