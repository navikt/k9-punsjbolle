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
    ) : RutingGrunnlag {
        if (harSakSomSøker(søker, fraOgMed, correlationId)) {
            return RutingGrunnlag(søker = true)
        }
        if (pleietrengende?.let { harSakSomPleietrengende(it, fraOgMed, correlationId) } == true) {
            return RutingGrunnlag(søker = false, pleietrengende = true)
        }
        return RutingGrunnlag(
            søker = false,
            pleietrengende = false,
            annenPart = annenPart?.let { harSakSomSøker(it, fraOgMed, correlationId) } ?: false
        )
    }

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

        return JSONObject(response).inneholderAktuelleSakerEllerVedtak().also { if (it) {
            secureLogger.info("Fant saker/vedtak i Infotrygd som søker for Identitetsnummer=[$identitetsnummer], FraOgMed=[$fraOgMed], Response=[$response]")
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

        return JSONArray(response).inneholderAktuelleVedtak().also { if (it) {
            secureLogger.info("Fant vedtak i Infotrygd som pleietrengende for Identitetsnummer=[$identitetsnummer], FraOgMed=[$fraOgMed], Response=[$response]")
        }}
    }

    internal companion object {
        private val secureLogger = LoggerFactory.getLogger("tjenestekall")
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsjbolle"
        private const val CorrelationIdHeaderKey = "Nav-Callid"
        private val relevanteBehandlingstema = listOf(
            "OP", "PB", "PP", "PN"
        )

        private fun JSONObject.getJSONArrayOrEmptyArray(key: String) = when (has(key) && get(key) is JSONArray) {
            true -> getJSONArray(key)
            false -> JSONArray()
        }

        private fun JSONObject.stringOrNull(key: String) = when (has(key) && get(key) is String) {
            true -> getString(key)
            else -> null
        }

        private fun JSONObject.hasJSONObject(key: String) = has(key) && get(key) is JSONObject

        private fun JSONObject.inneholderAktuelle(key: String) =
            getJSONArrayOrEmptyArray(key)
            .asSequence()
            .map { it as JSONObject }
            .filter { it.hasJSONObject("tema") }
            .filter { "BS" == it.getJSONObject("tema").stringOrNull("kode") }
            .filter { it.hasJSONObject("behandlingstema") }
            .filter { relevanteBehandlingstema.contains(it.getJSONObject("behandlingstema").stringOrNull("kode")) }
            .toList()
            .isNotEmpty()

        internal fun JSONObject.inneholderAktuelleSakerEllerVedtak() =
            inneholderAktuelle("saker") || inneholderAktuelle("vedtak")

        internal fun JSONArray.inneholderAktuelleVedtak() =
            map { it as JSONObject }
            .map { it.inneholderAktuelle("vedtak") }
            .any { it }
    }
}
