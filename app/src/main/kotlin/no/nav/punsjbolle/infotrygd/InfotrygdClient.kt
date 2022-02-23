package no.nav.punsjbolle.infotrygd

import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPost
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.jsonBody
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.punsjbolle.AzureAwareClient
import no.nav.punsjbolle.CorrelationId
import no.nav.punsjbolle.Identitetsnummer
import no.nav.punsjbolle.Json.arrayOrEmptyArray
import no.nav.punsjbolle.Json.objectOrEmptyObject
import no.nav.punsjbolle.Json.stringOrNull
import no.nav.punsjbolle.Søknadstype
import no.nav.punsjbolle.infotrygd.InfotrygdClient.Companion.Behandlingstema.Companion.relevanteBehandlingstemaer
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
    pingUrl = URI("$baseUrl/actuator/health")
) {

    private val HentSakerUrl = URI("$baseUrl/saker")
    private val HentVedtakForPleietrengende = URI("$baseUrl/vedtakForPleietrengende")

    internal suspend fun harLøpendeSakSomInvolvererEnAv(
        fraOgMed: LocalDate,
        søker: Identitetsnummer,
        pleietrengende: Identitetsnummer?,
        annenPart: Identitetsnummer?,
        søknadstype: Søknadstype,
        correlationId: CorrelationId,
    ): RutingGrunnlag {
        if (harSakSomSøker(søker, fraOgMed, søknadstype, correlationId)) {
            return RutingGrunnlag(søker = true)
        }
        if (pleietrengende?.let { harSakSomPleietrengende(it, fraOgMed, søknadstype, correlationId) } == true) {
            return RutingGrunnlag(søker = false, pleietrengende = true)
        }
        return RutingGrunnlag(
            søker = false,
            pleietrengende = false,
            annenPart = annenPart?.let { harSakSomSøker(it, fraOgMed, søknadstype, correlationId) } ?: false
        )
    }

    private suspend fun harSakSomSøker(
        identitetsnummer: Identitetsnummer,
        fraOgMed: LocalDate,
        søknadstype: Søknadstype,
        correlationId: CorrelationId
    ): Boolean {

        val url = URI("$HentSakerUrl")
        val jsonPayload = fnrFomjsonPayload(identitetsnummer, fraOgMed)

        val (httpStatusCode, response) = url.httpPost {
            it.header(HttpHeaders.Authorization, authorizationHeader())
            it.header(CorrelationIdHeaderKey, "$correlationId")
            it.header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            it.accept(ContentType.Application.Json)
            it.jsonBody(jsonPayload)
        }.readTextOrThrow()

        require(httpStatusCode.isSuccess()) {
            "Feil fra Infotrygd. URL=[$HentSakerUrl], HttpStatusCode=[${httpStatusCode.value}], Response=[$response]"
        }

        return JSONObject(response).inneholderAktuelleSakerEllerVedtak(søknadstype).also { if (it) {
            secureLogger.info("Fant saker/vedtak i Infotrygd som søker for Identitetsnummer=[$identitetsnummer], FraOgMed=[$fraOgMed], Response=[$response]")
        }}
    }

    private suspend fun harSakSomPleietrengende(
        identitetsnummer: Identitetsnummer,
        fraOgMed: LocalDate,
        søknadstype: Søknadstype,
        correlationId: CorrelationId
    ): Boolean {

        val url = URI("$HentVedtakForPleietrengende")
        val jsonPayload = fnrFomjsonPayload(identitetsnummer, fraOgMed)

        val (httpStatusCode, response) = url.httpPost {
            it.header(HttpHeaders.Authorization, authorizationHeader())
            it.header(CorrelationIdHeaderKey, "$correlationId")
            it.header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            it.accept(ContentType.Application.Json)
            it.jsonBody(jsonPayload)
        }.readTextOrThrow()

        require(httpStatusCode.isSuccess()) {
            "Feil fra Infotrygd. URL=[$HentVedtakForPleietrengende], HttpStatusCode=[${httpStatusCode.value}], Response=[$response]"
        }

        return JSONArray(response).inneholderAktuelleVedtak(søknadstype).also { if (it) {
            secureLogger.info("Fant vedtak i Infotrygd som pleietrengende for Identitetsnummer=[$identitetsnummer], FraOgMed=[$fraOgMed], Response=[$response]")
        }}
    }

    internal companion object {
        private val secureLogger = LoggerFactory.getLogger("tjenestekall")
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsjbolle"
        private const val CorrelationIdHeaderKey = "Nav-Callid"

        private enum class Behandlingstema(val infotrygdVerdi: String) {
            PleiepengerSyktBarnGammelOrdning("PB"),
            PleiepengerILivetsSluttfase("PP"),
            Opplæringspenger("OP"),
            Omsorgspenger("OM");

            companion object {
                fun Søknadstype.relevanteBehandlingstemaer() = when (this) {
                    Søknadstype.PleiepengerSyktBarn -> listOf(PleiepengerSyktBarnGammelOrdning)
                    Søknadstype.OmsorgspengerUtbetaling_Korrigering -> listOf(Omsorgspenger)
                    Søknadstype.OmsorgspengerKroniskSyktBarn -> listOf(Omsorgspenger)
                    Søknadstype.OmsorgspengerMidlertidigAlene -> listOf(Omsorgspenger)
                    Søknadstype.OmsorgspengerAleneOmsorg -> listOf(Omsorgspenger)
                    Søknadstype.Omsorgspenger -> listOf(Omsorgspenger)
                    Søknadstype.PleiepengerLivetsSluttfase -> listOf(PleiepengerILivetsSluttfase)
                }.map { it.infotrygdVerdi }
            }
        }

        private enum class Tema(val infotrygdVerdi: String) {
            BarnsSykdom("BS");

            companion object {
                internal val relevanteTemaer = listOf(
                    BarnsSykdom
                ).map { it.infotrygdVerdi }
            }
        }

        private enum class Resultat(val infotrygdVerdi: String) {
            HenlagtEllerBortfalt("HB")
        }

        private fun JSONObject.inneholderAktuelle(key: String, søknadstype: Søknadstype) =
            arrayOrEmptyArray(key)
                .asSequence()
                .map { it as JSONObject }
                .filter { Tema.relevanteTemaer.contains(it.objectOrEmptyObject("tema").stringOrNull("kode")) }
                .filter {
                    søknadstype.relevanteBehandlingstemaer()
                        .contains(it.objectOrEmptyObject("behandlingstema").stringOrNull("kode"))
                }
                // Om den er henlagt/bortfalt og ikke har noen opphørsdato er det aldri gjort noen utbetalinger
                .filterNot {
                    Resultat.HenlagtEllerBortfalt.infotrygdVerdi == it.objectOrEmptyObject("resultat")
                        .stringOrNull("kode") && it.stringOrNull("opphoerFom") == null
                }
                .toList()
                .isNotEmpty()

        internal fun JSONObject.inneholderAktuelleSakerEllerVedtak(søknadstype: Søknadstype) =
            inneholderAktuelle("saker", søknadstype) || inneholderAktuelle("vedtak", søknadstype)

        internal fun JSONArray.inneholderAktuelleVedtak(søknadstype: Søknadstype) =
            map { it as JSONObject }
                .map { it.inneholderAktuelle("vedtak", søknadstype) }
                .any { it }

        internal fun fnrFomjsonPayload(identitetsnummer: Identitetsnummer, fraOgMed: LocalDate): String {
            return """
            {
              "fnr": "$identitetsnummer",
              "fom": "$fraOgMed"
            }
            """.trimIndent()
        }
    }
}
