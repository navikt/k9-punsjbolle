package no.nav.punsjbolle.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.k9.rapid.behov.Behov
import no.nav.punsjbolle.HentBehov
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.punsjbolle.LeggTilBehov
import no.nav.punsjbolle.joark.Journalpost
import no.nav.punsjbolle.joark.Journalpost.Companion.tidligstMottattJournalpost
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal object SendPunsjetSøknadTilK9SakMelding :
    HentBehov<SendPunsjetSøknadTilK9SakMelding.SendPunsjetSøknadTilK9SakGrunnlag>,
    LeggTilBehov<SendPunsjetSøknadTilK9SakMelding.SendPunsjetSøknadTilK9SakGrunnlag> {

    internal data class SendPunsjetSøknadTilK9SakGrunnlag(
        internal val saksnummer: K9Saksnummer,
        internal val journalpostId: JournalpostId,
        internal val mottatt: LocalDateTime,
        internal val referanse: String,
        internal val brevkode: String) {

        internal companion object {
            internal fun Set<Journalpost>.somSendSøknadTilK9SakGrunnlag(saksnummer: K9Saksnummer) = this.tidligstMottattJournalpost().let { journalpost ->
                SendPunsjetSøknadTilK9SakGrunnlag(
                    saksnummer = saksnummer,
                    journalpostId = journalpost.journalpostId,
                    mottatt = journalpost.opprettet,
                    referanse = journalpost.referanse(),
                    brevkode = journalpost.brevkode()
                )
            }

            private const val Punsjbolle = "Punsjbolle"
            private val logger = LoggerFactory.getLogger(SendPunsjetSøknadTilK9SakGrunnlag::class.java)
            private fun Journalpost.brevkode() = when (brevkode) {
                null -> Punsjbolle.also { logger.warn("JournalpostId=[$journalpostId] mangler brevkode, defaulter til Brevkode=[$it]") }
                else -> brevkode
            }
            private fun Journalpost.referanse() = when (eksternReferanse) {
                null -> "$Punsjbolle-$journalpostId".also { logger.warn("JournalpostId=[$journalpostId] mangler kanalReferanse, setter Referanse=[$it]") }
                else -> eksternReferanse
            }
        }
    }

    override fun behov(behovInput: SendPunsjetSøknadTilK9SakGrunnlag): Behov {
        return Behov(behovNavn, mapOf(
            "saksnummer" to "${behovInput.saksnummer}",
            "journalpostId" to "${behovInput.journalpostId}",
            "mottatt" to "${behovInput.mottatt}",
            "referanse" to behovInput.referanse,
            "brevkode" to behovInput.brevkode
        ))
    }

    internal val behovNavn = "SendPunsjetSøknadTilK9Sak"

    override fun validateBehov(packet: JsonMessage) {
        packet.interestedIn(
            SaksnummerKey,
            JournalpostIdKey,
            MottattKey,
            ReferanseKey,
            BrevkodeKey
        )
    }

    override fun hentBehov(packet: JsonMessage): SendPunsjetSøknadTilK9SakGrunnlag {
        return SendPunsjetSøknadTilK9SakGrunnlag(
            saksnummer = packet[SaksnummerKey].asText().somK9Saksnummer(),
            journalpostId = packet[JournalpostIdKey].asText().somJournalpostId(),
            mottatt = packet[MottattKey].asLocalDateTime(),
            referanse = packet[ReferanseKey].asText(),
            brevkode = packet[BrevkodeKey].asText()
        )
    }

    private val SaksnummerKey = "@behov.$behovNavn.saksnummer"
    private val JournalpostIdKey = "@behov.$behovNavn.journalpostId"
    private val MottattKey = "@behov.$behovNavn.mottatt"
    private val ReferanseKey = "@behov.$behovNavn.referanse"
    private val BrevkodeKey = "@behov.$behovNavn.brevkode"
    override val mdcPaths = mapOf("k9_saksnummer" to SaksnummerKey)
}