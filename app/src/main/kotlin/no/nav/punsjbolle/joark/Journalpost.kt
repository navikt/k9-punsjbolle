package no.nav.punsjbolle.joark

import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.K9Saksnummer
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal data class Journalpost(
    internal val journalpostId: JournalpostId,
    private val journalposttype: String,
    private val journalpoststatus: String,
    internal val eksternReferanse: String?,
    internal val brevkode: String?,
    internal val opprettet: LocalDateTime,
    internal val sak: Sak?) {

    init {
        if (sak == null && erJournalført()) {
            logger.warn("Journalpost $journalpostId har status $journalpoststatus, men ingen sakskobling. Mest sannsynlig journalført mot generell sak.")
        }
    }

    internal fun erKnyttetTil(saksnummer: K9Saksnummer) : Boolean {
        return sak?.let { "K9" == it.fagsaksystem && "$saksnummer" == it.fagsakId }?:false
    }

    private fun erInngående() = journalposttype == "I"
    private fun erJournalført() = journalførtStatuser.contains(journalpoststatus)

    internal fun kanKnyttesTilSak() : Boolean {
        return journalpoststatus == "MOTTATT" && journalposttype == "I"
    }

    internal data class Sak (
        internal val fagsaksystem: String,
        internal val fagsakId: String
    )

    internal companion object {
        private val logger = LoggerFactory.getLogger(Journalpost::class.java)
        private val journalførtStatuser = listOf("JOURNALFOERT", "FERDIGSTILT")

        internal fun Set<Journalpost>.tidligstOpprettetJournalpost() =
            minByOrNull { it.opprettet }!!

        internal suspend fun Journalpost?.kanSendesTilK9Sak(eksisterendeSaksnummer: suspend () -> K9Saksnummer?) : Boolean {
            if (this == null || kanKnyttesTilSak()) return true
            val saksnummer = eksisterendeSaksnummer().also {
                logger.info("Eksisterende K9Saksnummer=[$it]")
            }
            return saksnummer != null && erKnyttetTil(saksnummer)
        }

        internal suspend fun Journalpost.kanKopieres(eksisterendeSaksnummer: suspend () -> K9Saksnummer?) =
            erInngående() && kanSendesTilK9Sak(eksisterendeSaksnummer)
    }
}