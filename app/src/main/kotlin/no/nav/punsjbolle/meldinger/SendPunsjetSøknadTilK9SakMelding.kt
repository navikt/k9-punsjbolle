package no.nav.punsjbolle.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.punsjbolle.HentBehov
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.punsjbolle.LeggTilBehov

internal object SendPunsjetSøknadTilK9SakMelding :
    HentBehov<K9Saksnummer>,
    LeggTilBehov<K9Saksnummer> {

    internal data class SendPunsjetSøknadTilK9SakGrunnlag(
        internal val saksnummer: K9Saksnummer,
        internal val journalpostId: JournalpostId,
        internal val referanse: String
    )

    override fun behov(behovInput: K9Saksnummer): Behov {
        return Behov(behovNavn, mapOf(
            "saksnummer" to "$behovInput"
        ))
    }

    internal val behovNavn = "SendPunsjetSøknadTilK9Sak"

    override fun validateBehov(packet: JsonMessage) {
        packet.interestedIn(
            SaksnummerKey
        )
    }

    override fun hentBehov(packet: JsonMessage) =
        packet[SaksnummerKey].asText().somK9Saksnummer()

    private val SaksnummerKey = "@behov.$behovNavn.saksnummer"
    override val mdcPaths = mapOf("k9_saksnummer" to SaksnummerKey)
}