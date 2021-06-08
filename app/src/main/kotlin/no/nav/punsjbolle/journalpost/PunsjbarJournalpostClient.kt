package no.nav.punsjbolle.journalpost

import no.nav.punsjbolle.AktørId
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.Søknadstype
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.slf4j.LoggerFactory

internal class PunsjbarJournalpostClient {

    internal fun send(punsjbarJournalpost: PunsjbarJournalpost) {
        val (key, value) = map(punsjbarJournalpost)
        logger.info("Sender Key=[$key], Value=$value")
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(PunsjbarJournalpost::class.java)

        internal fun map(punsjbarJournalpost: PunsjbarJournalpost) : Pair<String, String> {
            @Language("JSON")
            val baseDto = """
                {
                  "journalpostId": "${punsjbarJournalpost.journalpostId}",
                  "aktørId": "${punsjbarJournalpost.aktørId}",
                  "ytelse": "${punsjbarJournalpost.søknadstype.k9YtelseType}",
                  "type": "${punsjbarJournalpost.type}"
                }
            """.trimIndent()

            val dto = JSONObject(baseDto).also { baseJson ->
                punsjbarJournalpost.ekstraInfo.keys.intersect(baseJson.keySet()).takeIf { it.isNotEmpty() }?.also {
                    throw IllegalArgumentException("Kan ikke overstyre verdiene for $it")
                }
                punsjbarJournalpost.ekstraInfo.forEach { (key, value) -> baseJson.put(key, value) }
            }.toString()

            return "${punsjbarJournalpost.journalpostId}" to dto
        }
    }
}

internal interface PunsjbarJournalpost {
    val journalpostId: JournalpostId
    val aktørId: AktørId
    val søknadstype: Søknadstype
    val type: String
    val ekstraInfo: Map<String, Map<*,*>>
}

internal data class KopiertJournalpost(
    override val journalpostId: JournalpostId,
    override val aktørId: AktørId,
    override val søknadstype: Søknadstype,
    private val opprinneligJournalpostId: JournalpostId) : PunsjbarJournalpost {
    override val type = "KOPI"
    override val ekstraInfo = mapOf(
        "opprinneligJournalpost" to mapOf(
            "journalpostId" to "$opprinneligJournalpostId"
        )
    )
}