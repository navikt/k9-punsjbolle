package no.nav.punsjbolle

import no.nav.punsjbolle.meldinger.OpprettGosysJournalføringsoppgaverMelding.somJournalpostType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class JournalpostTypeTest {

    /**
     * Må finnes en mapping mellom denne typen og Gosys-interne verdier her for at det faktisk skal bli oppgaver.
     * https://github.com/navikt/omsorgspenger-journalforing/blob/master/src/main/kotlin/no/nav/omsorgspenger/oppgave/OppgaveAttributer.kt#L4
     * TODO: Legge til mapping på disse 4 i omsorgspenger-journalforing
     */
    @Test
    fun `mapping fra søknadstype til journalpostType `() {
        assertEquals("PleiepengerSyktBarnSøknad", Søknadstype.PleiepengerSyktBarn.somJournalpostType())
        assertEquals("OmsorgspengerUtbetalingSøknad", Søknadstype.OmsorgspengerUtbetaling.somJournalpostType())
        assertEquals("OmsorgspengerKroniskSyktBarnSøknad", Søknadstype.OmsorgspengerKroniskSyktBarn.somJournalpostType())
        assertEquals("OmsorgspengerMidlertidigAleneSøknad", Søknadstype.OmsorgspengerMidlertidigAlene.somJournalpostType())
    }
}