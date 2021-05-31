package no.nav.punsjbolle.k9sak

import no.nav.punsjbolle.k9sak.K9SakClient.Companion.inneholderMatchendeFagsak
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MatchFagsakMappingTest {

    @Test
    fun `mapper matching av fagsaker`() {
        assertTrue(HarFagsaker.inneholderMatchendeFagsak())
        assertFalse(IngenFagsaker.inneholderMatchendeFagsak())
    }

    private companion object {
        private const val IngenFagsaker = "[]"
        private const val HarFagsaker = """[{"foo": "bar"},{"foo":"bar2"}]"""
    }
}