package no.nav.punsjbolle.meldinger

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.k9.rapid.behov.Behov
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.LeggTilBehov
import no.nav.punsjbolle.søknad.PunsjetSøknadMelding

internal object JournalførJsonMelding : LeggTilBehov<JournalførJsonMelding.JournalførJson>{

    internal data class JournalførJson(
        internal val punsjetSøknad: PunsjetSøknadMelding.PunsjetSøknad,
        internal val saksnummer: K9Saksnummer
    )

    override fun behov(behovInput: JournalførJson): Behov {
        val søknad : Map<String, *> = jacksonObjectMapper().convertValue(behovInput.punsjetSøknad.søknadJson)
        val json = linkedMapOf(
            "nøkkelinformasjon" to mapOf(
                "punsjetAv" to behovInput.punsjetSøknad.saksbehandler,
                "saksnummer" to "${behovInput.saksnummer}"
            ),
            "innsending" to søknad
        )
        return Behov(
            navn = "JournalførJson@punsjInnsending",
            input = mapOf(
                "json" to json,
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
}