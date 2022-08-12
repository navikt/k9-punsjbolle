package no.nav.punsjbolle.journalpost

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.punsjbolle.HentBehov
import no.nav.punsjbolle.Identitetsnummer
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.Søknadstype

internal object KopierPunsjbarJournalpostMelding : HentBehov<KopierPunsjbarJournalpostMelding.KopierPunsjbarJournalpost> {
    data class KopierPunsjbarJournalpost(
        val versjon: String,
        val fra: Identitetsnummer,
        val til: Identitetsnummer,
        val pleietrengende: Identitetsnummer?,
        val annenPart: Identitetsnummer?,
        val journalpostId: JournalpostId,
        val søknadstype: Søknadstype) {
        internal val identitetsnummer = setOfNotNull(fra, til, pleietrengende, annenPart)
        init {
            require(pleietrengende != fra && pleietrengende != til) { "Pleietrengende kan ikke være samme person som fra/til." }
            require(fra != annenPart) { "Fra og annenPart kan ikke være samme person." }
        }
    }

    override fun validateBehov(packet: JsonMessage) {
        packet.interestedIn(
            VersjonKey,
            FraKey,
            TilKey,
            PleietrengendeKey,
            AnnenPartKey,
            JournalpostIdKey,
            SøknadstypeKey
        )
    }

    override fun hentBehov(packet: JsonMessage, brevkode: String?): KopierPunsjbarJournalpost {
        return KopierPunsjbarJournalpost(
            versjon = packet[VersjonKey].asText(),
            fra = packet[FraKey].asText().somIdentitetsnummer(),
            til = packet[TilKey].asText().somIdentitetsnummer(),
            pleietrengende = when (packet[PleietrengendeKey].isMissingOrNull()) {
                true -> null
                false -> packet[PleietrengendeKey].asText().somIdentitetsnummer()
            },
            annenPart = when (packet[AnnenPartKey].isMissingOrNull()) {
                true -> null
                false -> packet[AnnenPartKey].asText().somIdentitetsnummer()
            },
            journalpostId = packet[JournalpostIdKey].asText().somJournalpostId(),
            søknadstype = Søknadstype.valueOf(packet[SøknadstypeKey].asText())
        )
    }


    internal val behovNavn = "KopierPunsjbarJournalpost"
    private val VersjonKey = "@behov.$behovNavn.versjon"
    private val FraKey = "@behov.$behovNavn.fra"
    private val TilKey = "@behov.$behovNavn.til"
    private val PleietrengendeKey = "@behov.$behovNavn.pleietrengende"
    private val AnnenPartKey = "@behov.$behovNavn.annenPart"
    private val JournalpostIdKey = "@behov.$behovNavn.journalpostId"
    private val SøknadstypeKey = "@behov.$behovNavn.søknadstype"

    override val mdcPaths = mapOf("journalpost_id" to JournalpostIdKey)
}
