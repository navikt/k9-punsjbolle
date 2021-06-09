package no.nav.punsjbolle.journalpost

import no.nav.punsjbolle.AktørId
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.Søknadstype
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.slf4j.LoggerFactory

internal class PunsjbarJournalpostClient(
    private val kafkaProducer: KafkaProducer<String, String>) {

    internal fun send(punsjbarJournalpost: PunsjbarJournalpost) {
        val (key, value) = map(punsjbarJournalpost)
        kotlin.runCatching {
            kafkaProducer.send(ProducerRecord(Topic, key, value)).get()
        }.onSuccess { metadata ->
            logger.info("Innsending OK, Key=[$key], Topic=[${metadata.topic()}], Offset=[${metadata.offset()}], Partition=[${metadata.partition()}]")
        }.onFailure { throwable ->
            throw IllegalStateException("Feil ved innsending, Key=[$key], Topic=[$Topic]", throwable)
        }
    }

    internal fun close() = logger.info("Lukker KafkaProducer").also { kafkaProducer.close() }

    internal companion object {
        private const val Topic = "k9saksbehandling.punsjbar-journalpost"
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