package no.nav.punsjbolle.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.punsjbolle.HentBehov
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.punsjbolle.LeggTilBehov
import no.nav.punsjbolle.joark.Journalpost
import no.nav.punsjbolle.joark.Journalpost.Companion.tidligstOpprettetJournalpost

internal object SendPunsjetSøknadTilK9SakMelding :
    HentBehov<SendPunsjetSøknadTilK9SakMelding.SendPunsjetSøknadTilK9SakGrunnlag>,
    LeggTilBehov<SendPunsjetSøknadTilK9SakMelding.SendPunsjetSøknadTilK9SakGrunnlag> {

    internal data class SendPunsjetSøknadTilK9SakGrunnlag(
        internal val saksnummer: K9Saksnummer,
        internal val journalpostId: JournalpostId,
        internal val referanse: String) {

        internal companion object {
            internal fun Set<Journalpost>.somSendSøknadTilK9SakGrunnlag(
                saksnummer: K9Saksnummer,
                behovssekvensId: String) = this.tidligstOpprettetJournalpost().let { journalpost ->
                SendPunsjetSøknadTilK9SakGrunnlag(
                    saksnummer = saksnummer,
                    journalpostId = journalpost.journalpostId,
                    referanse = journalpost.eksternReferanse ?: behovssekvensId
                )
            }
        }
    }

    override fun behov(behovInput: SendPunsjetSøknadTilK9SakGrunnlag): Behov {
        return Behov(behovNavn, mapOf(
            "saksnummer" to "${behovInput.saksnummer}",
            "journalpostId" to "${behovInput.journalpostId}",
            "referanse" to behovInput.referanse
        ))
    }

    internal val behovNavn = "SendPunsjetSøknadTilK9Sak"

    override fun validateBehov(packet: JsonMessage) {
        packet.interestedIn(
            SaksnummerKey,
            JournalpostIdKey,
            ReferanseKey
        )
    }

    override fun hentBehov(packet: JsonMessage): SendPunsjetSøknadTilK9SakGrunnlag {
        return SendPunsjetSøknadTilK9SakGrunnlag(
            saksnummer = packet[SaksnummerKey].asText().somK9Saksnummer(),
            journalpostId = packet[JournalpostIdKey].asText().somJournalpostId(),
            referanse = packet[ReferanseKey].asText()
        )
    }

    private val SaksnummerKey = "@behov.$behovNavn.saksnummer"
    private val JournalpostIdKey = "@behov.$behovNavn.journalpostId"
    private val ReferanseKey = "@behov.$behovNavn.referanse"
    override val mdcPaths = mapOf("k9_saksnummer" to SaksnummerKey)
}