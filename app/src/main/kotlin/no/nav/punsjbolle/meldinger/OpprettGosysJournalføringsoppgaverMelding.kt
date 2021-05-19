package no.nav.punsjbolle.meldinger

import no.nav.k9.rapid.behov.Behov
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.LeggTilBehov
import no.nav.punsjbolle.søknad.PunsjetSøknadMelding

internal object OpprettGosysJournalføringsoppgaverMelding : LeggTilBehov<Pair<PunsjetSøknadMelding.PunsjetSøknad, Set<JournalpostId>>> {

    internal val behovNavn = "OpprettGosysJournalføringsoppgaver"

    override fun behov(behovInput: Pair<PunsjetSøknadMelding.PunsjetSøknad, Set<JournalpostId>>): Behov {
        val (søknad, journalpostIder) = behovInput
        return Behov(
            navn = behovNavn,
            input = mapOf(
                "identitetsnummer" to "${søknad.søker}",
                "berørteIdentitetsnummer" to søknad.identitetsnummer.minus(søknad.søker).map { "$it" },
                "journalpostIder" to journalpostIder.map { "$it" },
                "journalpostType" to søknad.søknadstype.journalpostType
            )
        )
    }
}