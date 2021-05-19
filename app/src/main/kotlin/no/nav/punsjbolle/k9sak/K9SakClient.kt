package no.nav.punsjbolle.k9sak

import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPost
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.jsonBody
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.stringBody
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.punsjbolle.AzureAwareClient
import no.nav.punsjbolle.CorrelationId
import no.nav.punsjbolle.Identitetsnummer
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.punsjbolle.Søknadstype
import no.nav.punsjbolle.meldinger.HentK9SaksnummerMelding
import no.nav.punsjbolle.meldinger.SendPunsjetSøknadTilK9SakMelding
import no.nav.punsjbolle.ruting.RutingGrunnlag
import no.nav.punsjbolle.søknad.PunsjetSøknadMelding
import org.intellij.lang.annotations.Language
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate
import java.util.*

internal class K9SakClient(
    baseUrl: URI,
    accessTokenClient: AccessTokenClient,
    scopes: Set<String>
) : AzureAwareClient(
    navn = "K9SakClient",
    accessTokenClient = accessTokenClient,
    scopes = scopes,
    pingUrl = URI("$baseUrl/internal/health/isReady")) {

    private val HentSaksnummerUrl = URI("$baseUrl/api/fordel/fagsak/opprett")
    private val SendInnSøknadUrl = URI("$baseUrl/api/fordel/journalposter")
    private val MatchFagsak = URI("$baseUrl/api/fagsak/match")

    internal suspend fun hentSaksnummer(
        grunnlag: HentK9SaksnummerMelding.HentK9SaksnummerGrunnlag,
        correlationId: CorrelationId) : K9Saksnummer {

        // https://github.com/navikt/k9-sak/blob/3.1.30/kontrakt/src/main/java/no/nav/k9/sak/kontrakt/mottak/FinnEllerOpprettSak.java#L23
        @Language("JSON")
        val dto = """
            {
                "ytelseType": "${grunnlag.søknadstype.k9YtelseType}",
                "aktørId": "${grunnlag.søker}",
                "pleietrengendeAktørId": ${grunnlag.pleietrengende?.let { """"$it"""" }},
                "relatertPersonAktørId": ${grunnlag.annenPart?.let { """"$it"""" }},
                "periode": {
                    "fom": ${grunnlag.periode.fom?.let { """"$it"""" }},
                    "tom": ${grunnlag.periode.tom?.let { """"$it"""" }}
                }
            }
        """.trimIndent()

        val (httpStatusCode, response) = HentSaksnummerUrl.httpPost {
            it.header(HttpHeaders.Authorization, authorizationHeader())
            it.header(CorrelationIdHeaderKey, "$correlationId")
            it.header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            it.accept(ContentType.Application.Json)
            it.jsonBody(dto)
        }.readTextOrThrow()

        require(httpStatusCode == HttpStatusCode.OK) {
            "Feil fra K9Sak. URL=[$HentSaksnummerUrl], HttpStatusCode=[${httpStatusCode.value}], Response=[$response]"
        }

        return JSONObject(response).getString("saksnummer").somK9Saksnummer()
    }

    internal suspend fun sendInnSøknad(
        søknad: PunsjetSøknadMelding.PunsjetSøknad,
        grunnlag: SendPunsjetSøknadTilK9SakMelding.SendPunsjetSøknadTilK9SakGrunnlag,
        correlationId: CorrelationId) {

        // https://github.com/navikt/k9-sak/blob/3.1.30/kontrakt/src/main/java/no/nav/k9/sak/kontrakt/mottak/JournalpostMottakDto.java#L31
        @Language("JSON")
        val dto = """
            [{
                "saksnummer": "${grunnlag.saksnummer}",
                "journalpostId": "${grunnlag.journalpostId}",
                "ytelseType": {
                    "kode": "${søknad.søknadstype.k9YtelseType}",
                    "kodeverk": "FAGSAK_YTELSE"
                },
                "kanalReferanse": "${grunnlag.referanse}",
                "type": "${søknad.søknadstype.k9Type}",
                "forsendelseMottattTidspunkt": "${grunnlag.mottatt}",
                "forsendelseMottatt": "${grunnlag.mottatt.toLocalDate()}",
                "payload": "${Base64.getUrlEncoder().encodeToString(søknad.søknadJson.toString().toByteArray())}"
            }]
        """.trimIndent()

        val (httpStatusCode, response) = SendInnSøknadUrl.httpPost {
            it.header(HttpHeaders.Authorization, authorizationHeader())
            it.header(CorrelationIdHeaderKey, "$correlationId")
            it.header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            it.accept(ContentType.Application.Json)
            it.jsonArrayBody(dto)
        }.readTextOrThrow()

        require(httpStatusCode.isSuccess()) {
            "Feil fra K9Sak. URL=[$SendInnSøknadUrl], HttpStatusCode=[${httpStatusCode.value}], Response=[$response]"
        }
    }

    internal suspend fun harLøpendeSakSomInvolvererEnAv(
        søker: Identitetsnummer,
        pleietrengende: Identitetsnummer?,
        annenPart: Identitetsnummer?,
        fraOgMed: LocalDate,
        søknadstype: Søknadstype,
        correlationId: CorrelationId
    ) = RutingGrunnlag(
        søker = finnesMatchendeFagsak(søker = søker, fraOgMed = fraOgMed, correlationId = correlationId, søknadstype = søknadstype),
        pleietrengende = pleietrengende?.let { finnesMatchendeFagsak(pleietrengende = it, fraOgMed = fraOgMed, correlationId = correlationId, søknadstype = søknadstype) } ?: false,
        annenPart = annenPart?.let { finnesMatchendeFagsak(søker = it, fraOgMed = fraOgMed, correlationId = correlationId, søknadstype = søknadstype) } ?: false
    )

    private suspend fun finnesMatchendeFagsak(
        søker: Identitetsnummer? = null,
        pleietrengende: Identitetsnummer? = null,
        annenPart: Identitetsnummer? = null,
        fraOgMed: LocalDate,
        søknadstype: Søknadstype,
        correlationId: CorrelationId
    ): Boolean {

        // https://github.com/navikt/k9-sak/tree/3.1.30/kontrakt/src/main/java/no/nav/k9/sak/kontrakt/fagsak/MatchFagsak.java#L26
        @Language("JSON")
        val dto = """
        {
            "ytelseType": {
                "kode": "${søknadstype.k9YtelseType}",
                "kodeverk": "FAGSAK_YTELSE"
            },
            "periode": {
                "fom": "$fraOgMed"
            },
            "bruker": ${søker?.let { """"$it"""" }},
            "pleietrengendeIdenter": ${pleietrengende.jsonArray()},
            "relatertPersonIdenter": ${annenPart.jsonArray()}
        }
        """.trimIndent()
        
        val (httpStatusCode, response) = MatchFagsak.httpPost {
            it.header(HttpHeaders.Authorization, authorizationHeader())
            it.header(CorrelationIdHeaderKey, "$correlationId")
            it.header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            it.accept(ContentType.Application.Json)
            it.jsonBody(dto)
        }.readTextOrThrow()

        require(httpStatusCode.isSuccess()) {
            "Feil fra K9Sak. URL=[$MatchFagsak], HttpStatusCode=[${httpStatusCode.value}], Response=[$response]"
        }

        return response.inneholderMatchendeFagsak().also { if (it) {
            secureLogger.info("Fant sak(er) i K9sak, Request=[${JSONObject(dto)}], Response=[$response]")
        }}
    }

    internal companion object {
        private val secureLogger = LoggerFactory.getLogger("tjenestekall")
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsjbolle"
        private const val CorrelationIdHeaderKey = "Nav-Callid"

        private fun HttpRequestBuilder.jsonArrayBody(json: String) =
            stringBody(string = JSONArray(json).toString(), contentType = ContentType.Application.Json)

        private fun Identitetsnummer?.jsonArray() = when (this) {
            null -> "[]"
            else -> """["$this"]"""
        }

        internal fun String.inneholderMatchendeFagsak() = JSONArray(this)
            .asSequence()
            .map { it as JSONObject }
            .filterNot { it.getBoolean("skalBehandlesAvInfotrygd") }
            .filterNot { it.getString("status") == "AVSLU" }
            .toSet()
            .isNotEmpty()
    }
}