package no.nav.punsjbolle.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.punsjbolle.HentLøsning
import no.nav.punsjbolle.Identitetsnummer
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.LeggTilBehov

internal object KopierJournalpostForK9Melding :
    LeggTilBehov<KopierJournalpostForK9Melding.KopierJournalpostForK9>,
    HentLøsning<JournalpostId> {

    data class KopierJournalpostForK9(
        internal val fra: Part,
        internal val til: Part,
        internal val journalpostId: JournalpostId
    )

    data class Part(
        internal val identitetsnummer: Identitetsnummer,
        internal val saksnummer: K9Saksnummer
    )

    override fun behov(behovInput: KopierJournalpostForK9): Behov {
        return Behov(behovNavn, mapOf(
            "versjon" to "1.0.0",
            "journalpostId" to "${behovInput.journalpostId}",
            "fra" to mapOf(
                "identitetsnummer" to "${behovInput.fra.identitetsnummer}",
                "saksnummer" to "${behovInput.fra.saksnummer}"
            ),
            "til" to mapOf(
                "identitetsnummer" to "${behovInput.til.identitetsnummer}",
                "saksnummer" to "${behovInput.til.saksnummer}"
            )
        ))
    }

    override fun validateLøsning(packet: JsonMessage) {
        packet.interestedIn(journalpostIdKey)
    }

    override fun hentLøsning(packet: JsonMessage): JournalpostId {
        return packet[journalpostIdKey].asText().somJournalpostId()
    }

    internal const val behovNavn = "KopierJournalpostForK9"
    private const val journalpostIdKey = "@løsninger.$behovNavn.journalpostId"
}