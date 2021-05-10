package no.nav.punsjbolle.joark

import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.K9Saksnummer
import java.time.LocalDateTime

internal data class Journalpost(
    internal val journalpostId: JournalpostId,
    private val journalposttype: String,
    private val journalpoststatus: String,
    internal val eksternReferanse: String?,
    internal val brevkode: String?,
    internal val opprettet: LocalDateTime,
    internal val sak: Sak?) {

    internal fun erKnyttetTil(saksnummer: K9Saksnummer) : Boolean {
        return sak?.let { "K9" == it.fagsaksystem && "$saksnummer" == it.fagsakId }?:false
    }

    internal fun skalKnyttesTilSak() : Boolean {
        return sak == null && journalpoststatus == "MOTTATT" && journalposttype == "I"
    }

    internal data class Sak (
        internal val fagsaksystem: String,
        internal val fagsakId: String
    )

    internal companion object {
        internal fun Set<Journalpost>.tidligstMottattJournalpost() =
            minByOrNull { it.opprettet }!!
    }
}