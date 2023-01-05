package no.nav.punsjbolle.k9sak

import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPost
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.jsonBody
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.stringBody
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.punsjbolle.*
import no.nav.punsjbolle.Json.stringOrNull
import no.nav.punsjbolle.K9Saksnummer.Companion.somK9Saksnummer
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
import java.time.ZoneId
import java.util.*

internal class K9SakClient(
    baseUrl: URI,
    accessTokenClient: AccessTokenClient,
    scopes: Set<String>
) : AzureAwareClient(
    navn = "K9SakClient",
    accessTokenClient = accessTokenClient,
    scopes = scopes,
    pingUrl = URI("$baseUrl/internal/health/isReady")
) {

    private val HentEllerOpprettSaksnummerUrl = URI("$baseUrl/api/fordel/fagsak/opprett")
    private val HentSaksnummerUrl = URI("$baseUrl/api/fagsak/siste")
    private val SendInnSøknadUrl = URI("$baseUrl/api/fordel/journalposter")
    private val MatchFagsakUrl = URI("$baseUrl/api/fagsak/match")
    private val PleiepengerSyktBarnUnntakslisteUrl = URI("$baseUrl/api/fordel/psb-infotrygd/finnes")


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

    internal suspend fun hentEksisterendeSaksnummer(
        grunnlag: HentK9SaksnummerMelding.HentK9SaksnummerGrunnlag,
        correlationId: CorrelationId
    ): K9Saksnummer? {

        // https://github.com/navikt/k9-sak/blob/3.2.10/kontrakt/src/main/java/no/nav/k9/sak/kontrakt/mottak/FinnSak.java#L46
        @Language("JSON")
        val dto = """
            {
                "ytelseType": "${grunnlag.søknadstype.k9YtelseType}",
                "aktørId": "${grunnlag.søker}",
                "pleietrengendeAktørId": ${grunnlag.pleietrengende?.let { """"$it"""" }},
                "relatertPersonAktørId": ${grunnlag.annenPart?.let { """"$it"""" }},
                "periode": {}
            }
        """.trimIndent()

        val (httpStatusCode, response) = post(
            dto = dto,
            correlationId = correlationId,
            uri = HentSaksnummerUrl
        )

        return when (httpStatusCode) {
            HttpStatusCode.OK -> response.saksnummer()
            HttpStatusCode.NoContent -> null
            else -> throw IllegalStateException(
                "Feil fra K9Sak. URL=[$HentSaksnummerUrl], HttpStatusCode=[${httpStatusCode.value}], Response=[$response]"
            )
        }
    }

    private suspend fun post(
        uri: URI,
        dto: String,
        correlationId: CorrelationId
    ): Pair<HttpStatusCode, String> {

        return uri.toString().httpPost {
            it.header(HttpHeaders.Authorization, authorizationHeader())
            it.header(CorrelationIdHeaderKey, "$correlationId")
            it.header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            it.accept(ContentType.Application.Json)
            it.jsonBody(dto)
        }.readTextOrThrow()
    }

    internal suspend fun sendInnSøknad(
        søknad: PunsjetSøknadMelding.PunsjetSøknad,
        grunnlag: SendPunsjetSøknadTilK9SakMelding.SendPunsjetSøknadTilK9SakGrunnlag,
        correlationId: CorrelationId
    ) {

        val forsendelseMottattTidspunkt = søknad.mottatt.withZoneSameInstant(Oslo).toLocalDateTime()

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
                "type": "${søknad.søknadstype.brevkode.kode}",
                "forsendelseMottattTidspunkt": "$forsendelseMottattTidspunkt",
                "forsendelseMottatt": "${forsendelseMottattTidspunkt.toLocalDate()}",
                "payload": "${Base64.getUrlEncoder().encodeToString(søknad.søknadJson.toString().toByteArray())}"
            }]
        """.trimIndent()

        val (httpStatusCode, response) = SendInnSøknadUrl.toString().httpPost {
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
    ): RutingGrunnlag {
        if (finnesMatchendeFagsak(
                søker = søker,
                fraOgMed = fraOgMed,
                correlationId = correlationId,
                søknadstype = søknadstype
            )
        ) {
            return RutingGrunnlag(søker = true)
        }
        if (pleietrengende?.let {
                finnesMatchendeFagsak(
                    pleietrengende = it,
                    fraOgMed = fraOgMed,
                    correlationId = correlationId,
                    søknadstype = søknadstype
                )
            } == true) {
            return RutingGrunnlag(søker = false, pleietrengende = true)
        }
        return RutingGrunnlag(
            søker = false,
            pleietrengende = false,
            annenPart = annenPart?.let {
                finnesMatchendeFagsak(
                    søker = it,
                    fraOgMed = fraOgMed,
                    correlationId = correlationId,
                    søknadstype = søknadstype
                )
            } ?: false
        )
    }

    private suspend fun finnesMatchendeFagsak(
        søker: Identitetsnummer? = null,
        pleietrengende: Identitetsnummer? = null,
        annenPart: Identitetsnummer? = null,
        @Suppress("UNUSED_PARAMETER") fraOgMed: LocalDate,
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
            "periode": {},
            "bruker": ${søker?.let { """"$it"""" }},
            "pleietrengendeIdenter": ${pleietrengende.jsonArray()},
            "relatertPersonIdenter": ${annenPart.jsonArray()}
        }
        """.trimIndent()

        val (httpStatusCode, response) = MatchFagsakUrl.toString().httpPost {
            it.header(HttpHeaders.Authorization, authorizationHeader())
            it.header(CorrelationIdHeaderKey, "$correlationId")
            it.header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            it.accept(ContentType.Application.Json)
            it.jsonBody(dto)
        }.readTextOrThrow()

        require(httpStatusCode.isSuccess()) {
            "Feil fra K9Sak. URL=[$MatchFagsakUrl], HttpStatusCode=[${httpStatusCode.value}], Response=[$response]"
        }

        val matchendeFagsak = JSONArray(response)
            .asSequence()
            .map { it as JSONObject }
            .filterNot {
                (it.getString("status") == "OPPR").also { erStatusOpprettet ->
                    if (erStatusOpprettet) {
                        logger.info("MatchendeFagsak: Filtrerer bort Saksnummer=${it.stringOrNull("saksnummer")} i Status=OPPR")
                    }
                }
            }
            .toSet()
            .isNotEmpty()

        return matchendeFagsak
    }

    internal suspend fun inngårIUnntaksliste(
        aktørIder: Set<AktørId>,
        correlationId: CorrelationId
    ): Boolean {

        // https://github.com/navikt/k9-sak/tree/3.2.7/web/src/main/java/no/nav/k9/sak/web/app/tjenester/fordeling/FordelRestTjeneste.java#L164
        // https://github.com/navikt/k9-sak/tree/3.2.7/kontrakt/src/main/java/no/nav/k9/sak/kontrakt/mottak/Akt%C3%B8rListeDto.java#L19
        val dto = JSONObject().also {
            it.put("aktører", JSONArray(aktørIder.map { aktørId -> "$aktørId" }))
        }.toString()

        val (httpStatusCode, response) = PleiepengerSyktBarnUnntakslisteUrl.toString().httpPost {
            it.header(HttpHeaders.Authorization, authorizationHeader())
            it.header(CorrelationIdHeaderKey, "$correlationId")
            it.header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            it.accept(ContentType.Application.Json)
            it.jsonBody(dto)
        }.readTextOrThrow()

        require(httpStatusCode.isSuccess() && (response == "true" || response == "false")) {
            "Feil fra K9Sak. URL=[$PleiepengerSyktBarnUnntakslisteUrl], HttpStatusCode=[${httpStatusCode.value}], Response=[$response]"
        }

        return (response == "true")
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(K9SakClient::class.java)
        private val Oslo = ZoneId.of("Europe/Oslo")

        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsjbolle"
        private const val CorrelationIdHeaderKey = "Nav-Callid"

        private fun HttpRequestBuilder.jsonArrayBody(json: String) =
            stringBody(string = JSONArray(json).toString(), contentType = ContentType.Application.Json)

        private fun Identitetsnummer?.jsonArray() = when (this) {
            null -> "[]"
            else -> """["$this"]"""
        }

        private fun String.saksnummer() = JSONObject(this).getString("saksnummer").somK9Saksnummer()
    }
}
