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
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.punsjbolle.meldinger.HentK9SaksnummerMelding
import no.nav.punsjbolle.meldinger.SendPunsjetSøknadTilK9SakMelding
import no.nav.punsjbolle.søknad.PunsjetSøknadMelding
import org.intellij.lang.annotations.Language
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
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

    internal suspend fun hentSaksnummer(
        grunnlag: HentK9SaksnummerMelding.HentK9SaksnummerGrunnlag,
        correlationId: CorrelationId) : K9Saksnummer {

        // https://github.com/navikt/k9-sak/blob/84987481324fbbfe128d71156eea1d02f0202845/kontrakt/src/main/java/no/nav/k9/sak/kontrakt/mottak/FinnEllerOpprettSak.java#L23
        @Language("JSON")
        val dto = """
            {
                "ytelseType": "${grunnlag.søknadstype.k9SakDto}",
                "aktørId": "${grunnlag.søker}",
                "pleietrengendeAktørId": ${grunnlag.pleietrengende?.let { "$it" }},
                "relatertPersonAktørId": ${grunnlag.annenPart?.let { "$it" }},
                "periode": {
                    "fom": ${grunnlag.periode.fom?.let { "$it" }},
                    "tom": ${grunnlag.periode.tom?.let { "$it" }}
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

        // https://github.com/navikt/k9-sak/blob/6678d3432980fc1dd40684b82a517ba3f43371d3/kontrakt/src/main/java/no/nav/k9/sak/kontrakt/mottak/JournalpostMottakDto.java#L31
        @Language("JSON")
        val dto = """
            [{
                "saksnummer": "${grunnlag.saksnummer}",
                "journalpostId": "${grunnlag.journalpostId}",
                "ytelseType": {
                    "kode": "${søknad.søknadstype.k9SakDto}",
                    "kodeverk": "FAGSAK_YTELSE"
                },
                "kanalReferanse": "${grunnlag.referanse}",
                "type": "${grunnlag.brevkode}",
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

    private companion object {
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsjbolle"
        private const val CorrelationIdHeaderKey = "Nav-Callid"
        private fun HttpRequestBuilder.jsonArrayBody(json: String) =
            stringBody(string = JSONArray(json).toString(), contentType = ContentType.Application.Json)
    }
}