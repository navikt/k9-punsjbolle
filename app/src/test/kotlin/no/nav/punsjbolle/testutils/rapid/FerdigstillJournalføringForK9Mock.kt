package no.nav.punsjbolle.testutils.rapid

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.punsjbolle.testutils.sisteMeldingSomJsonMessage

internal fun TestRapid.mockFerdigstillJournalføringForK9() {
    sendTestMessage(sisteMeldingSomJsonMessage().mockFerdigstillJournalføringForK9().toJson())
}

private fun JsonMessage.mockFerdigstillJournalføringForK9() = leggTilLøsning(
    behov = "FerdigstillJournalføringForK9"
)
