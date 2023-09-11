package no.nav.punsjbolle.api

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import no.nav.punsjbolle.*
import no.nav.punsjbolle.AktørId.Companion.somAktørId
import no.nav.punsjbolle.CorrelationId.Companion.somCorrelationId
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.Periode.Companion.forsikreLukketPeriode
import no.nav.punsjbolle.Periode.Companion.somPeriode
import no.nav.punsjbolle.api.Request.Companion.fraSøknadRequest
import no.nav.punsjbolle.api.Request.Companion.request
import no.nav.punsjbolle.joark.Journalpost
import no.nav.punsjbolle.joark.SafClient
import no.nav.punsjbolle.k9sak.K9SakClient
import no.nav.punsjbolle.meldinger.HentK9SaksnummerMelding
import no.nav.punsjbolle.sak.SakClient
import no.nav.punsjbolle.søknad.periode
import no.nav.punsjbolle.søknad.søknadstype
import org.slf4j.LoggerFactory

internal fun Route.SaksnummerApi(
    safClient: SafClient,
    k9SakClient: K9SakClient,
    sakClient: SakClient
) {

    suspend fun periodeOgJournalpost(request: Request): Pair<Periode, Journalpost?> {
        val journalpost = request.journalpostId?.let {
            safClient.hentJournalpost(
                journalpostId = it,
                correlationId = request.correlationId
            )
        }
        val periode = request.periode?.forsikreLukketPeriode() ?: journalpost!!.opprettet.toLocalDate().somPeriode()
        return periode to journalpost
    }

    suspend fun PipelineContext<Unit, ApplicationCall>.ruting(
        onK9Sak: suspend (request: Request, periode: Periode) -> Unit
    ) {
        val request = call.request()
        var (periode, _) = periodeOgJournalpost(request)
        onK9Sak(request, periode)

    }

    post("/saksnummer") {
        ruting(
            onK9Sak = { request, periode ->
                val saksnummer = k9SakClient.hentEllerOpprettSaksnummer(
                    correlationId = request.correlationId,
                    grunnlag = request.hentSaksnummerGrunnlag(periode)
                )
                logger.info("Hentet/Opprettet K9Saksnummer=[$saksnummer].")

                sakClient.forsikreSakskoblingFinnes( // Denne forsikrer att saken kommer opp som valg i Modia.
                    saksnummer = saksnummer,
                    søker = request.søker.aktørId,
                    correlationId = request.correlationId
                )

                call.respondText(
                    contentType = ContentType.Application.Json,
                    text = """{"saksnummer": "$saksnummer"}""",
                    status = HttpStatusCode.OK
                )
            }
        )
    }

    post("/saksnummer-fra-soknad") {
        val request = call.fraSøknadRequest()
        val (periode, _) = periodeOgJournalpost(request)

        val saksnummer = k9SakClient.hentEllerOpprettSaksnummer(
            correlationId = request.correlationId,
            grunnlag = request.hentSaksnummerGrunnlag(periode)
        )

        call.respondText(
            contentType = ContentType.Application.Json,
            text = """{"saksnummer": "$saksnummer"}""",
            status = HttpStatusCode.OK
        )
    }
}

private val logger = LoggerFactory.getLogger("no.nav.punsjbolle.api.SaksnummerApi")

internal data class Request(
    internal val correlationId: CorrelationId,
    internal val journalpostId: JournalpostId?,
    internal val søker: Part,
    internal val pleietrengende: Part?,
    internal val annenPart: Part?,
    internal val periode: Periode?,
    internal val søknadstype: Søknadstype
) {
    private val identitetsnumer =
        setOfNotNull(søker.identitetsnummer, pleietrengende?.identitetsnummer, annenPart?.identitetsnummer)
    internal val aktørIder = setOfNotNull(søker.aktørId, pleietrengende?.aktørId, annenPart?.aktørId)

    internal fun hentSaksnummerGrunnlag(periode: Periode) = HentK9SaksnummerMelding.HentK9SaksnummerGrunnlag(
        søknadstype = søknadstype,
        søker = søker.aktørId,
        pleietrengende = pleietrengende?.aktørId,
        annenPart = annenPart?.aktørId,
        periode = periode
    )

    init {
        val antallParter = listOfNotNull(søker, pleietrengende, annenPart).size
        require(antallParter == identitetsnumer.size && antallParter == aktørIder.size) {
            "Ugyldig request, Inneholdt $antallParter parter, men ${identitetsnumer.size} identitetsnummer og ${aktørIder.size} aktørIder."
        }
        require(journalpostId != null || periode != null) {
            "Må sette enten periode eller journalpostId"
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

        private fun ObjectNode.partOrNull(key: String) = objectNodeOrNull(key)?.let { part ->
            Part(
                identitetsnummer = part.stringOrNull("identitetsnummer")?.somIdentitetsnummer()
                    ?: throw IllegalStateException("Mangler identitetsnummer på $key"),
                aktørId = part.stringOrNull("aktørId")?.somAktørId()
                    ?: throw IllegalStateException("Mangler aktørId på $key")
            )
        }

        private fun ApplicationRequest.correlationId() =
            header(HttpHeaders.XCorrelationId)?.somCorrelationId()
                ?: throw IllegalStateException("Mangler correlationId")

        internal suspend fun ApplicationCall.request(): Request {
            val json = receive<ObjectNode>()
            return Request(
                correlationId = request.correlationId(),
                journalpostId = json.stringOrNull("journalpostId")?.somJournalpostId(),
                søknadstype = json.stringOrNull("søknadstype")?.let { Søknadstype.valueOf(it) }
                    ?: throw IllegalStateException("Mangler søknadstype"),
                søker = json.partOrNull("søker") ?: throw IllegalStateException("Mangler søker"),
                pleietrengende = json.partOrNull("pleietrengende"),
                annenPart = json.partOrNull("annenPart"),
                periode = json.stringOrNull("periode")?.somPeriode()
            )
        }

        internal suspend fun ApplicationCall.fraSøknadRequest(): Request {
            val json = receive<ObjectNode>()
            val søknad = json.get("søknad") as ObjectNode
            val søknadstype = søknad.søknadstype()
            return Request(
                correlationId = request.correlationId(),
                journalpostId = null,
                søknadstype = søknadstype,
                søker = json.partOrNull("søker") ?: throw IllegalStateException("Mangler søker"),
                pleietrengende = json.partOrNull("pleietrengende"),
                annenPart = json.partOrNull("annenPart"),
                periode = søknad.periode(søknadstype)
            )
        }
    }
}
