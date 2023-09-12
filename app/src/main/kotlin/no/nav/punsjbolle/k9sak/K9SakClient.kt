package no.nav.punsjbolle.k9sak

import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPost
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.jsonBody
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.punsjbolle.AzureAwareClient
import no.nav.punsjbolle.CorrelationId
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.punsjbolle.meldinger.HentK9SaksnummerMelding
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.net.URI

internal class K9SakClient(
    baseUrl: URI,
    accessTokenClient: AccessTokenClient,
    scopes: Set<String>
) : AzureAwareClient(
    navn = "K9SakClient",
    accessTokenClient = accessTokenClient,
    scopes = scopes
) {

    private val HentEllerOpprettSaksnummerUrl = URI("$baseUrl/api/fordel/fagsak/opprett")


    internal suspend fun hentEllerOpprettSaksnummer(
        grunnlag: HentK9SaksnummerMelding.HentK9SaksnummerGrunnlag,
        correlationId: CorrelationId
    ): K9Saksnummer {

        // https://github.com/navikt/k9-sak/blob/3.2.10/kontrakt/src/main/java/no/nav/k9/sak/kontrakt/mottak/FinnEllerOpprettSak.java#L49
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

        logger.info("Henter/Oppretter k9saksnummer ytelseType:[${grunnlag.søknadstype.k9YtelseType}] " +
                "for periode:[${grunnlag.periode.fom}/${grunnlag.periode.tom}].", correlationId)

        val (httpStatusCode, response) = post(
            dto = dto,
            correlationId = correlationId,
            uri = HentEllerOpprettSaksnummerUrl
        )

        require(httpStatusCode == HttpStatusCode.OK) {
            "Feil fra K9Sak. URL=[$HentEllerOpprettSaksnummerUrl], HttpStatusCode=[${httpStatusCode.value}], Response=[$response]"
        }

        return response.saksnummer()
    }

    private suspend fun post(
        uri: URI,
        dto: String,
        correlationId: CorrelationId
    ): Pair<HttpStatusCode, String> {

        return uri.toString().httpPost {
            it.header(HttpHeaders.Authorization, authorizationHeader())
            it.header(CorrelationIdHeaderKey, correlationId)
            it.header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            it.header("callId", correlationId)
            it.accept(ContentType.Application.Json)
            it.jsonBody(dto)
        }.readTextOrThrow()
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(K9SakClient::class.java)

        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsjbolle"
        private const val CorrelationIdHeaderKey = "Nav-Callid"

        private fun String.saksnummer() = JSONObject(this).getString("saksnummer").somK9Saksnummer()
    }
}
