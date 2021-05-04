package no.nav.punsjbolle.innsending

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.Ytelse
import no.nav.punsjbolle.*
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.K9Saksnummer.Companion.somK9Saksnummer
import java.time.LocalDate
import java.time.ZoneId

internal object PunsjetSøknadMelding : HentBehov<PunsjetSøknadMelding.PunsjetSøknad> {

    internal data class PunsjetSøknad(
        internal val versjon: String,
        internal val søknadId: String,
        internal val saksnummer: K9Saksnummer?,
        internal val søknadstype: Søknadstype,
        internal val søker: Identitetsnummer,
        internal val pleietrengende: Identitetsnummer?,
        internal val annenPart: Identitetsnummer?,
        internal val journalpostIder: Set<JournalpostId>,
        internal val fraOgMed: LocalDate,
        internal val tilOgMed: LocalDate?,
        internal val søknadJson: ObjectNode) {
        internal val identitetsnummer = setOfNotNull(søker, pleietrengende, annenPart)
        init {
            require(identitetsnummer.isNotEmpty()) { "Søknaden må gjelde minst en person." }
            require(journalpostIder.isNotEmpty()) { "Søknaden må være knyttet til minst en journalpost."}
        }
    }

    override fun validateBehov(packet: JsonMessage) {
        packet.interestedIn(
            VersjonKey,
            SaksnummerKey,
            SøknadKey,
            JournalpostIderKey
        )
    }

    override fun hentBehov(packet: JsonMessage): PunsjetSøknad {
        val søknadJson = packet[SøknadKey] as ObjectNode
        val (k9FormatYtelse, k9FormatSøknad) = validertK9FormatSøknad(søknadJson)
        val søknadsperiode : Periode? = k9FormatYtelse.søknadsperiode
        val mottaksdato: LocalDate = k9FormatSøknad.mottattDato.withZoneSameInstant(Oslo).toLocalDate()

        return PunsjetSøknad(
            versjon = packet[VersjonKey].asText(),
            søknadId = k9FormatSøknad.søknadId.id,
            søknadJson = søknadJson,
            saksnummer = when (packet[SaksnummerKey].isMissingOrNull()) {
                true -> null
                false -> packet[SaksnummerKey].asText().somK9Saksnummer()
            },
            søknadstype = Søknadstype.fraK9FormatYtelse(k9FormatYtelse),
            journalpostIder = (packet[JournalpostIderKey] as ArrayNode).map { it.asText().somJournalpostId() }.toSet(),
            fraOgMed = when (søknadsperiode) {
                null -> mottaksdato
                else -> søknadsperiode.fraOgMed
            },
            tilOgMed = when (søknadsperiode) {
                null -> null
                else -> søknadsperiode.tilOgMed
            },
            søker = k9FormatSøknad.søker.personIdent.verdi.somIdentitetsnummer(),
            annenPart = k9FormatYtelse.annenPart?.personIdent?.verdi?.somIdentitetsnummer(),
            pleietrengende = k9FormatYtelse.pleietrengende?.personIdent?.verdi?.somIdentitetsnummer()
        )
    }

    private fun validertK9FormatSøknad(søknadJson: ObjectNode) : Pair<Ytelse, Søknad> {
        val k9FormatSøknad = Søknad.SerDes.deserialize(søknadJson)
        val k9FormatYtelse : Ytelse = k9FormatSøknad.getYtelse()
        k9FormatYtelse.validator.forsikreValidert(k9FormatSøknad.getYtelse())
        return k9FormatYtelse to k9FormatSøknad
    }

    override val behovNavn = "PunsjetSøknad"
    private val VersjonKey = "@behov.$behovNavn.versjon"
    private val SaksnummerKey = "@behov.$behovNavn.saksnummer"
    private val SøknadKey = "@behov.$behovNavn.søknad"
    private val SøknadIdKey = "$SøknadKey.søknadId"
    private val JournalpostIderKey = "@behov.$behovNavn.journalpostIder"
    private val Oslo = ZoneId.of("Europe/Oslo")
    override val mdcPaths = mapOf("soknad_id" to SøknadIdKey, "k9_saksnummer" to SaksnummerKey)
}