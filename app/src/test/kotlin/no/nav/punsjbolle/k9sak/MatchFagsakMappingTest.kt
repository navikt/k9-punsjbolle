package no.nav.punsjbolle.k9sak

import no.nav.punsjbolle.k9sak.K9SakClient.Companion.inneholderMatchendeFagsak
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MatchFagsakMappingTest {

    @Test
    fun `mapper matching av fagsaker`() {
        assertTrue(matchFagsakResponse().inneholderMatchendeFagsak())
        assertTrue(matchFagsakResponse(status2 = "AVSLU").inneholderMatchendeFagsak())
        assertFalse(matchFagsakResponse(status1 = "AVSLU", status2 = "AVSLU").inneholderMatchendeFagsak())
        assertTrue(matchFagsakResponse(skalBehandlesAvInfotrygd2 = true).inneholderMatchendeFagsak())
        assertFalse(matchFagsakResponse(skalBehandlesAvInfotrygd1 = true, skalBehandlesAvInfotrygd2 = true).inneholderMatchendeFagsak())
        assertFalse("[]".inneholderMatchendeFagsak())
    }

    private companion object {
        @Language("JSON")
        private fun matchFagsakResponse(
            skalBehandlesAvInfotrygd1: Boolean = false,
            status1: String = "LOP",
            skalBehandlesAvInfotrygd2: Boolean = false,
            status2: String = "LOP") = """
            [
              {"skalBehandlesAvInfotrygd": $skalBehandlesAvInfotrygd1, "status": "$status1"},
              {"skalBehandlesAvInfotrygd": $skalBehandlesAvInfotrygd2, "status": "$status2"}
            ] 
        """.trimIndent()
    }
}