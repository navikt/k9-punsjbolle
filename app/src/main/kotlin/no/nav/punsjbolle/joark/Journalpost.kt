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

    private val erInngående = journalposttype == "I"
    private val erNotat = journalposttype == "N"
    internal val erFerdigstilt = ferdigstilteStatuser.contains(journalpoststatus)
    private val erMottatt = journalpoststatus == "MOTTATT"
    internal val kanKnyttesTilSak = erInngående && erMottatt
    internal val kanKopieres = erInngående || erNotat

    init {
        if (sak == null && erFerdigstilt) {
            logger.warn("Journalpost $journalpostId har status $journalpoststatus, men ingen sakskobling. Mest sannsynlig ferdigstilt mot generell sak.")
        }
    }

    internal fun erKnyttetTil(saksnummer: K9Saksnummer) : Boolean {
        val erKnyttetSak = sak?.let {
            val knyttetSak = "K9" == it.fagsaksystem && "$saksnummer" == it.fagsakId
            logger.info("K9 == ${it.fagsaksystem} && $saksnummer == ${it.fagsakId}")
            knyttetSak
        } ?: false
        logger.info("Er saksnummer($saksnummer) knyttet {}? {}", sak, if (erKnyttetSak) "ja" else "nei")
        return erKnyttetSak
    }

    internal data class Sak (
        internal val fagsaksystem: String,
        internal val fagsakId: String
    )

    internal companion object {
        private val logger = LoggerFactory.getLogger(Journalpost::class.java)
        private val ferdigstilteStatuser = listOf("JOURNALFOERT", "FERDIGSTILT")

        internal suspend fun Journalpost?.kanSendesTilK9Sak(eksisterendeSaksnummer: suspend () -> K9Saksnummer?) : Boolean {
            if (this == null || kanKnyttesTilSak) return true
            val saksnummer = eksisterendeSaksnummer().also {
                logger.info("Saknummer på journalposten: {}, eksisterende saksnummer fra k9: {}", this.sak, it)
            }
            return saksnummer != null && erKnyttetTil(saksnummer)
        }
    }
}
