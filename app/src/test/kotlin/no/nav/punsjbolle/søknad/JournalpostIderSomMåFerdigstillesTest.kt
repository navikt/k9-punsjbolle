package no.nav.punsjbolle.søknad

import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.punsjbolle.joark.Journalpost
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

internal class JournalpostIderSomMåFerdigstillesTest {

    @Test
    fun `Kombinasjon av alle gyldige journalpostkombinasjoner`() {
        assertEquals(alleGyldigeKombinasjonerJournalpostIderSomMåFerdigstilles, alleGyldigeKombinasjonerJournalposter.journalpostIderSomMåFerdigstilles())
    }

    @Test
    fun `Knyttet til annen sak feiler`() {
        val knyttetTilFeilSak = nyJournalpost(
            journalpoststatus = "JOURNALFOERT",
            journalposttype = "I",
            sak = K9Sak.copy(fagsakId = "SAK456")
        )
        assertThrows<IllegalStateException> {
            alleGyldigeKombinasjonerJournalposter.plus(knyttetTilFeilSak).journalpostIderSomMåFerdigstilles()
        }
    }

    private companion object {
        private val K9Saksnummer = "SAK123".somK9Saksnummer()
        private val K9Sak = Journalpost.Sak(fagsaksystem = "K9", fagsakId = "$K9Saksnummer")
        private var JournalpostId = 1000000000

        private fun nyJournalpost(
            journalpoststatus: String,
            journalposttype: String,
            sak: Journalpost.Sak? = null
        ) = Journalpost(
            brevkode = null,
            opprettet = LocalDateTime.now(),
            eksternReferanse = null,
            journalpostId = "${JournalpostId++}".somJournalpostId(),
            journalpoststatus = journalpoststatus,
            journalposttype = journalposttype,
            sak = sak
        )

        val mottattIngående = nyJournalpost(
            journalpoststatus = "MOTTATT",
            journalposttype = "I"
        )
        val journalførtInngåendeKnyttetTilSak = nyJournalpost(
            journalpoststatus = "JOURNALFOERT",
            journalposttype = "I",
            sak = K9Sak
        )
        val inngåendeKnyttetTilSakUkjentStatus = nyJournalpost(
            journalpoststatus = "NOE_ANNET",
            journalposttype = "I",
            sak = K9Sak
        )
        val ferdigstiltNotatKnyttetTilSak = nyJournalpost(
            journalpoststatus = "FERDIGSTILT",
            journalposttype = "N",
            sak = K9Sak
        )

        val notatKnyttetTilSakUkjentStatus = nyJournalpost(
            journalpoststatus = "NOE_HELT_ANNET",
            journalposttype = "N",
            sak = K9Sak
        )

        val alleGyldigeKombinasjonerJournalposter = setOf(
            mottattIngående,
            journalførtInngåendeKnyttetTilSak,
            inngåendeKnyttetTilSakUkjentStatus,
            ferdigstiltNotatKnyttetTilSak,
            notatKnyttetTilSakUkjentStatus
        )

        val alleGyldigeKombinasjonerJournalpostIderSomMåFerdigstilles = setOf(
            mottattIngående.journalpostId,
            inngåendeKnyttetTilSakUkjentStatus.journalpostId,
            notatKnyttetTilSakUkjentStatus.journalpostId
        )

        private fun Set<Journalpost>.journalpostIderSomMåFerdigstilles() =
            PunsjetSøknadTilK9Sak.journalpostIderSomMåFerdigstilles(
                saksnummer = K9Saksnummer,
                journalposter = this
            )
    }
}