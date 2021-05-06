package no.nav.punsjbolle.meldinger

import no.nav.k9.rapid.behov.Behov
import no.nav.punsjbolle.Identitetsnummer
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.LeggTilBehov

internal object FerdigstillJournalføringForK9Melding :
    LeggTilBehov<Triple<Identitetsnummer, K9Saksnummer, Set<JournalpostId>>> {

    override fun behov(behovInput: Triple<Identitetsnummer, K9Saksnummer, Set<JournalpostId>>): Behov {
        return Behov(behovNavn, mapOf(
            "identitetsnummer" to "${behovInput.first}",
            "saksnummer" to "${behovInput.second}",
            "journalpostIder" to behovInput.third.map { "$it" }
        ))
    }

    internal val behovNavn = "FerdigstillJournalføringForK9"
}