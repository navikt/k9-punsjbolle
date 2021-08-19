package no.nav.punsjbolle.testutils.rapid

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.river.leggTilLøsning

internal fun TestRapid.mockFerdigstillJournalføringForK9OgJournalførJson() {
    sendTestMessage(sisteMeldingSomJsonMessage().mockFerdigstillJournalføringForK9().mockJournalførJson().toJson())
}

private fun JsonMessage.mockFerdigstillJournalføringForK9() = leggTilLøsning(
    behov = "FerdigstillJournalføring@punsjInnsending"
)

private fun JsonMessage.mockJournalførJson() = leggTilLøsning(
    behov = "JournalførJson@punsjInnsending",
    løsning = mapOf("journalpostId" to "666555111")
)
