package no.nav.punsjbolle.testutils.rapid

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.punsjbolle.JournalpostId

internal fun TestRapid.mockKopierJournalpostForK9(journalpostId: JournalpostId) {
    sendTestMessage(sisteMeldingSomJsonMessage().mockKopierJournalpostForK9(journalpostId).toJson())
}

private fun JsonMessage.mockKopierJournalpostForK9(
    journalpostId: JournalpostId
) = leggTilLøsning(
    behov = "KopierJournalpost",
    løsning = mapOf(
        "journalpostId" to "$journalpostId"
    )
)
