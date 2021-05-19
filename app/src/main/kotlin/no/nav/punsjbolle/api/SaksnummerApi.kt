package no.nav.punsjbolle.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.punsjbolle.*
import no.nav.punsjbolle.AktørId
import no.nav.punsjbolle.AktørId.Companion.somAktørId
import no.nav.punsjbolle.CorrelationId
import no.nav.punsjbolle.CorrelationId.Companion.somCorrelationId
import no.nav.punsjbolle.Identitetsnummer
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.Søknadstype
import no.nav.punsjbolle.api.Request.Companion.request
import no.nav.punsjbolle.joark.SafClient
import no.nav.punsjbolle.k9sak.K9SakClient
import no.nav.punsjbolle.meldinger.HentK9SaksnummerMelding
import no.nav.punsjbolle.ruting.RutingService
import org.json.JSONObject
import java.time.LocalDate

internal fun Route.SaksnummerApi(
    rutingService: RutingService,
    safClient: SafClient,
    k9SakClient: K9SakClient) {

    post("/saksnummer") {
        val request = call.request()

        val fraOgMed = request.fraOgMed ?: safClient.hentJournalpost(
            journalpostId = request.journalpostId!!,
            correlationId = request.correlationId
        ).opprettet.toLocalDate()

        val destinasjon = rutingService.destinasjon(
            søker = request.søker.identitetsnummer,
            pleietrengende = request.pleietrengende?.identitetsnummer,
            annenPart = request.annenPart?.identitetsnummer,
            fraOgMed = fraOgMed,
            søknadstype = request.søknadstype,
            correlationId = request.correlationId
        )

        when (destinasjon) {
            RutingService.Destinasjon.K9Sak -> {
                val saksnummer = k9SakClient.hentSaksnummer(
                    correlationId = request.correlationId,
                    grunnlag = HentK9SaksnummerMelding.HentK9SaksnummerGrunnlag(
                        søknadstype = request.søknadstype,
                        søker = request.søker.aktørId,
                        pleietrengende = request.pleietrengende?.aktørId,
                        annenPart = request.annenPart?.aktørId,
                        periode = Periode(fom = fraOgMed, tom = null)
                    )
                )
                call.respondText(
                    contentType = ContentType.Application.Json,
                    text = """{"saksnummer": "$saksnummer"}""",
                    status = HttpStatusCode.OK
                )
            }
            RutingService.Destinasjon.Infotrygd -> {
                call.respond(HttpStatusCode.Conflict)
            }
        }
    }
}

internal data class Request(
    val correlationId: CorrelationId,
    val journalpostId: JournalpostId?,
    val søker: Part,
    val pleietrengende: Part?,
    val annenPart: Part?,
    val fraOgMed: LocalDate?,
    val søknadstype: Søknadstype) {

    init { require(journalpostId != null || fraOgMed != null) {
        "Må sette enten journalpostId eller fraOgMed"
    }}

    internal data class Part(
        val aktørId: AktørId,
        val identitetsnummer: Identitetsnummer
    )

    internal companion object {
        private fun JSONObject.stringOrNull(key: String) = when (has(key) && get(key) is String) {
            true -> getString(key)
            false -> null
        }
        private fun JSONObject.partOrNull(key: String) = kotlin.runCatching { getJSONObject(key).let { part ->
            Part(aktørId = part.getString("aktørId").somAktørId(), identitetsnummer = part.getString("identitetsnummer").somIdentitetsnummer())
        }}.fold(onSuccess = {it}, onFailure = {null})
        private suspend fun ApplicationCall.json() = kotlin.runCatching { JSONObject(receiveText()) }.fold(onSuccess = {it}, onFailure = {JSONObject()})
        internal suspend fun ApplicationCall.request() : Request {
            val json = json()
            return Request(
                correlationId = request.header(HttpHeaders.XCorrelationId)?.somCorrelationId() ?: throw IllegalStateException("Mangler correlationId"),
                journalpostId = json.stringOrNull("journalpostId")?.somJournalpostId(),
                søknadstype = json.stringOrNull("søknadstype")?.let { Søknadstype.valueOf(it) } ?: throw IllegalStateException("Mangler søknadstype"),
                søker = json.partOrNull("søker") ?: throw IllegalStateException("Mangler søker"),
                pleietrengende = json.partOrNull("pleietrengende"),
                annenPart = json.partOrNull("annenPart"),
                fraOgMed = json.stringOrNull("fraOgMed")?.let { LocalDate.parse(it) }
            )
        }
    }
}
