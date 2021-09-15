package no.nav.punsjbolle.meldinger

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.punsjbolle.HentLøsning
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.LeggTilBehov
import no.nav.punsjbolle.Søknadstype
import no.nav.punsjbolle.søknad.PunsjetSøknadMelding

internal object JournalførJsonMelding : LeggTilBehov<JournalførJsonMelding.JournalførJson>, HentLøsning<JournalpostId?> {

    internal const val behovNavn = "JournalførJson@punsjInnsending"
    private val journalpostIdKey = "@løsninger.${behovNavn}.journalpostId"

    internal data class JournalførJson(
        internal val punsjetSøknad: PunsjetSøknadMelding.PunsjetSøknad,
        internal val saksnummer: K9Saksnummer
    )

    override fun behov(behovInput: JournalførJson): Behov {
        val søknad: Map<String, *> = jacksonObjectMapper().convertValue(
            behovInput.punsjetSøknad.søknadJson.manipulerSøknadsJson(
                søknadstype = behovInput.punsjetSøknad.søknadstype
            )
        )
        return Behov(
            navn = behovNavn,
            input = mapOf(
                "json" to søknad.plus(mapOf(
                    "_punsjetAv" to behovInput.punsjetSøknad.saksbehandler,
                    "_saksnummer" to "${behovInput.saksnummer}"
                )),
                "identitetsnummer" to "${behovInput.punsjetSøknad.søker}",
                "fagsystem" to "K9",
                "saksnummer" to "${behovInput.saksnummer}",
                "brevkode" to "K9_PUNSJ_INNSENDING",
                "mottatt" to "${behovInput.punsjetSøknad.mottatt}",
                "farge" to "#C1B5D0",
                "tittel" to "Innsending fra Punsj",
                "avsender" to mapOf(
                    "navn" to behovInput.punsjetSøknad.saksbehandler
                )
            )
        )
    }

    private fun ObjectNode.objectNodeOrNull(key: String) = when (hasNonNull(key) && get(key) is ObjectNode) {
        true -> get(key) as ObjectNode
        false -> null
    }

    private fun String.renameKeys(fra: String, til: String) = replace(
        oldValue = """"$fra":""",
        newValue = """"$til":""",
        ignoreCase = false
    )

    private fun String.renameValues(fraKey: String, fraValue: String, tilValue: String) = replace(
        oldValue = """"$fraKey":"$fraValue"""",
        newValue = """"$fraKey":"$tilValue"""",
        ignoreCase = false
    )

    private fun String.renameLand() : String {
        var current = this
        Land.values().forEach { land -> current = current.renameValues("land", land.name, land.navn) }
        return current
    }

    internal fun ObjectNode.manipulerSøknadsJson(søknadstype: Søknadstype) : ObjectNode {
        // Fjerner informasjon på toppnivå
        val søknad = deepCopy()
        søknad.remove(setOf("versjon", "språk"))
        // Fjerner informasjon i "ytelse"
        søknad.objectNodeOrNull("ytelse")?.also { ytelse ->
            ytelse.remove(setOf("type"))
        }
        return "$søknad"
            .renameKeys("ytelse", søknadstype.name)
            .renameKeys("mottattDato", "mottatt")
            .renameKeys("søknadsperiode", "søknadsperioder")
            .renameKeys("endringsperiode", "endringsperioder")
            .renameKeys("norskIdentitetsnummer", "identitetsnummer")
            .renameKeys("arbeidstakerList", "arbeidstakere")
            .renameKeys("frilanserArbeidstidInfo", "frilanser")
            .renameKeys("jobberFortsattSomFrilans", "jobberFortsattSomFrilanser")
            .renameKeys("selvstendigNæringsdrivendeArbeidstidInfo", "selvstendigNæringsdrivende")
            .renameKeys("arbeidstidInfo", "arbeidstid")
            .renameKeys("arbeidAktivitet", "arbeid")
            .renameKeys("virksomhetNavn", "virksomhetsnavn")
            .renameKeys("dataBruktTilUtledning", "overordnet")
            .renameKeys("etablertTilsynTimerPerDag", "etablertTilsynPerDag")
            .renameKeys("jobberNormaltTimerPerDag", "normalArbeidstidPerDag")
            .renameKeys("faktiskArbeidTimerPerDag", "faktiskArbeidstidPerDag")
            .renameKeys("timerPleieAvBarnetPerDag", "pleieAvBarnetPerDag")
            .renameKeys("inneholderInfomasjonSomIkkeKanPunsjes", "inneholderInformasjonSomIkkeKanPunsjes")
            .renameLand()
            .let { jacksonObjectMapper().readTree(it) as ObjectNode }
    }

    override fun validateLøsning(packet: JsonMessage) {
        packet.interestedIn(journalpostIdKey)
    }

    override fun hentLøsning(packet: JsonMessage) =
        packet[journalpostIdKey].asText().somJournalpostId()
}