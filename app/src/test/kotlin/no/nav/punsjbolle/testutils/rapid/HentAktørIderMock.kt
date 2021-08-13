package no.nav.punsjbolle.testutils.rapid

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.punsjbolle.Identitetsnummer

internal fun TestRapid.mockHentAktørIder(identitetsnummer: Set<Identitetsnummer>) {
    sendTestMessage(sisteMeldingSomJsonMessage().mockHentAktørIder(identitetsnummer).toJson())
}

private fun JsonMessage.mockHentAktørIder(
    identitetsnummer: Set<Identitetsnummer>) = leggTilLøsning(
    behov = "HentPersonopplysninger",
    løsning = mapOf(
        "personopplysninger" to identitetsnummer.map { "$it" }.associateWith { mapOf(
            "aktørId" to "9$it"
        )}
    )
)
