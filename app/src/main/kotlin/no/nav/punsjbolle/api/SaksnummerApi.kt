package no.nav.punsjbolle.api

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.punsjbolle.AktørId
import no.nav.punsjbolle.AktørId.Companion.somAktørId
import no.nav.punsjbolle.CorrelationId
import no.nav.punsjbolle.CorrelationId.Companion.somCorrelationId
import no.nav.punsjbolle.Identitetsnummer
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.Periode
import no.nav.punsjbolle.Periode.Companion.forsikreLukketPeriode
import no.nav.punsjbolle.Periode.Companion.somPeriode
import no.nav.punsjbolle.Søknadstype
import no.nav.punsjbolle.api.Request.Companion.request
import no.nav.punsjbolle.joark.Journalpost
import no.nav.punsjbolle.joark.SafClient
import no.nav.punsjbolle.k9sak.K9SakClient
import no.nav.punsjbolle.meldinger.HentK9SaksnummerMelding
import no.nav.punsjbolle.ruting.RutingService
import no.nav.punsjbolle.sak.SakClient
import org.slf4j.LoggerFactory

internal fun Route.SaksnummerApi(
    rutingService: RutingService,
    safClient: SafClient,
    k9SakClient: K9SakClient,
    sakClient: SakClient) {

    post("/saksnummer") {
        val request = call.request()

        val journalpost = safClient.hentJournalpost(
            journalpostId = request.journalpostId,
            correlationId = request.correlationId
        )

        val periode = request.periode?.forsikreLukketPeriode() ?: journalpost.opprettet.toLocalDate().somPeriode()

        val destinasjon = rutingService.destinasjon(
            søker = request.søker.identitetsnummer,
            pleietrengende = request.pleietrengende?.identitetsnummer,
            annenPart = request.annenPart?.identitetsnummer,
            fraOgMed = periode.fom!!,
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
                        periode = periode
                    )
                )

                logger.info("Hentet K9Saksnummer=[$saksnummer] for JournalpostId=[${request.journalpostId}], Periode=[$periode], Søknadstype=[${request.søknadstype.name}]")

                if (journalpost.kanRutesTilK9Sak(saksnummer)) {
                    sakClient.forsikreSakskoblingFinnes(
                        saksnummer = saksnummer,
                        søker = request.søker.aktørId,
                        correlationId = request.correlationId
                    )
                    call.respondText(
                        contentType = ContentType.Application.Json,
                        text = """{"saksnummer": "$saksnummer"}""",
                        status = HttpStatusCode.OK
                    )
                } else {
                    call.respondConflict(
                        feil = "ikke-støttet-journalpost",
                        detaljer = "$journalpost kan ikke rutes inn til K9Sak på saksummer $saksnummer."
                    )
                }
            }
            RutingService.Destinasjon.Infotrygd -> {
                call.respondConflict(
                    feil = "må-behandles-i-infotrygd",
                    detaljer = "Minst en part har en løpende sak i Infotrygd."
                )
            }
        }
    }
}

private val logger = LoggerFactory.getLogger("no.nav.punsjbolle.api.SaksnummerApi")

private fun Journalpost.kanRutesTilK9Sak(saksnummer: K9Saksnummer) =
    erKnyttetTil(saksnummer) || kanKnyttesTilSak()

private suspend fun ApplicationCall.respondConflict(feil: String, detaljer: String) {
    logger.warn("Feil=[$feil], Detaljer=[$detaljer]")
    respondText(
        contentType = ContentType.Application.Json,
        text = """
            {
                "status": 409,
                "type": "punsjbolle://$feil",
                "details": "$detaljer"
            }
        """.trimIndent(),
        status = HttpStatusCode.Conflict
    )
}

internal data class Request(
    internal val correlationId: CorrelationId,
    internal val journalpostId: JournalpostId,
    internal val søker: Part,
    internal val pleietrengende: Part?,
    internal val annenPart: Part?,
    internal val periode: Periode?,
    internal val søknadstype: Søknadstype) {
    
    init {
        val antallParter = listOfNotNull(søker, pleietrengende, annenPart).size
        val antallUnikeIdentitetsnummer = setOfNotNull(søker.identitetsnummer, pleietrengende?.identitetsnummer, annenPart?.identitetsnummer).size
        val antallUnikeAktørIder = setOfNotNull(søker.aktørId, pleietrengende?.aktørId, annenPart?.aktørId).size
        require(antallParter == antallUnikeIdentitetsnummer && antallParter == antallUnikeAktørIder) {
            "Ugylidig request, Inneholdt $antallParter parter, men $antallUnikeIdentitetsnummer identitetsnummer og $antallUnikeAktørIder aktørIder."
        }
    }

    internal data class Part(
        val aktørId: AktørId,
        val identitetsnummer: Identitetsnummer
    )

    internal companion object {
        private fun ObjectNode.stringOrNull(key: String) = when (hasNonNull(key) && get(key).isTextual) {
            true -> get(key).textValue()
            false -> null
        }

        private fun ObjectNode.objectNodeOrNull(key: String) = when (hasNonNull(key) && get(key).isObject) {
            true -> get(key) as ObjectNode
            false -> null
        }

        private fun ObjectNode.partOrNull(key: String) = objectNodeOrNull(key)?.let { part -> Part(
            identitetsnummer = part.stringOrNull("identitetsnummer")?.somIdentitetsnummer() ?: throw IllegalStateException("Mangler identitetsnummer på $key"),
            aktørId = part.stringOrNull("aktørId")?.somAktørId() ?: throw IllegalStateException("Mangler aktørId på $key")
        )}

        internal suspend fun ApplicationCall.request() : Request {
            val json = receive<ObjectNode>()
            return Request(
                correlationId = request.header(HttpHeaders.XCorrelationId)?.somCorrelationId() ?: throw IllegalStateException("Mangler correlationId"),
                journalpostId = json.stringOrNull("journalpostId")?.somJournalpostId() ?: throw IllegalStateException("Mangler journalpostId"),
                søknadstype = json.stringOrNull("søknadstype")?.let { Søknadstype.valueOf(it) } ?: throw IllegalStateException("Mangler søknadstype"),
                søker = json.partOrNull("søker") ?: throw IllegalStateException("Mangler søker"),
                pleietrengende = json.partOrNull("pleietrengende"),
                annenPart = json.partOrNull("annenPart"),
                periode = json.stringOrNull("periode")?.somPeriode()
            )
        }
    }
}
