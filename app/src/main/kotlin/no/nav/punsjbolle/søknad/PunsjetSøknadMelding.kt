package no.nav.punsjbolle.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.punsjbolle.*
import no.nav.punsjbolle.K9Saksnummer.Companion.somK9Saksnummer
import java.time.ZonedDateTime

internal object PunsjetSøknadMelding :
    HentBehov<PunsjetSøknadMelding.PunsjetSøknad> {

    internal data class PunsjetSøknad(
        internal val versjon: String,
        internal val søknadId: String,
        internal val saksnummer: K9Saksnummer?,
        internal val søknadstype: Søknadstype,
        internal val søker: Identitetsnummer,
        internal val pleietrengende: Identitetsnummer?,
        internal val annenPart: Identitetsnummer?,
        internal val journalpostIder: Set<JournalpostId>,
        internal val periode: Periode,
        internal val mottatt: ZonedDateTime,
        internal val søknadJson: ObjectNode,
        internal val saksbehandler: String) {
        internal val identitetsnummer = setOfNotNull(søker, pleietrengende, annenPart)
        init {
            require(identitetsnummer.isNotEmpty()) { "Søknaden må gjelde minst en person." }
            require(journalpostIder.isNotEmpty()) { "Søknaden må være knyttet til minst en journalpostId."}
            require(listOfNotNull(søker, pleietrengende, annenPart).size == identitetsnummer.size) {
                "Søknaden må ha unike personer som søker/pleietrengende/annenPart."
            }
        }
    }

    override fun validateBehov(packet: JsonMessage) {
        packet.interestedIn(
            VersjonKey,
            SøknadKey,
            SaksbehandlerKey,
            SøknadstypeKey
        )
    }

    override fun hentBehov(packet: JsonMessage): PunsjetSøknad {
        val søknadJson = packet[SøknadKey] as ObjectNode
        val søknadsType = packet[SøknadstypeKey].asText()
        val brevkode = Brevkode.fraKode(søknadsType)

        return søknadJson.somPunsjetSøknad(
            versjon = packet[VersjonKey].asText(),
            saksbehandler = packet[SaksbehandlerKey].saksbehandler(),
            brevkode = brevkode,
            saksnummer = when (packet[SaksnummerKey].isMissingOrNull()) {
                true -> null
                false -> packet[SaksnummerKey].asText().somK9Saksnummer()
            }
        )
    }

    private fun JsonNode.saksbehandler() = when (isMissingOrNull()) {
        true -> "n/a"
        false -> asText()
    }

    internal val behovNavn = "PunsjetSøknad"
    private val VersjonKey = "@behov.$behovNavn.versjon"
    private val SaksnummerKey = "@behov.$behovNavn.saksnummer"
    private val SøknadstypeKey = "@behov.$behovNavn.søknadstype"
    private val SaksbehandlerKey = "@behov.$behovNavn.saksbehandler"
    private val SøknadKey = "@behov.$behovNavn.søknad"
    private val SøknadIdKey = "$SøknadKey.søknadId"

    override val mdcPaths = mapOf(
        "soknad_id" to SøknadIdKey,
        "k9_saksnummer" to SaksnummerKey,
        "saksbehandler" to SaksbehandlerKey
    )
}
