package no.nav.punsjbolle.søknad

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.Periode
import no.nav.punsjbolle.Søknadstype

internal fun ObjectNode.somPunsjetSøknad(
    versjon: String,
    saksnummer: K9Saksnummer?) : PunsjetSøknadMelding.PunsjetSøknad {

    val ytelse = get("ytelse") as ObjectNode
    val søknadstype = Søknadstype.fraK9FormatYtelsetype(ytelse.get("type").asText())

    return when (søknadstype) {
        Søknadstype.PleiepengerSyktBarn -> fraPleiepengerSyktBarn(
            versjon = versjon,
            saksnummer = saksnummer
        )
        else -> throw IllegalStateException("Mangler mapping av ${søknadstype.name}")
    }
}

private fun ObjectNode.fraPleiepengerSyktBarn(
    versjon: String,
    saksnummer: K9Saksnummer?) : PunsjetSøknadMelding.PunsjetSøknad {
    return PunsjetSøknadMelding.PunsjetSøknad(
        versjon = versjon,
        saksnummer = saksnummer,
        søknadId = søknadsId(),
        journalpostIder = journalpostIder(),
        søknadstype = Søknadstype.PleiepengerSyktBarn,
        søker = søker(),
        pleietrengende = barn(),
        annenPart = null,
        søknadJson = this,
        periode = periode()
    )
}

private fun ObjectNode.søknadsId() = get("søknadId").asText()
private fun ObjectNode.journalpostIder() = (get("journalposter") as ArrayNode).map { it as ObjectNode }.map { it.get("journalpostId").asText().somJournalpostId() }.toSet()
private fun ObjectNode.søker() = get("søker").get("norskIdentitetsnummer").asText().somIdentitetsnummer()
private fun ObjectNode.barn() = get("ytelse").get("barn")?.get("norskIdentitetsnummer")?.asText()?.somIdentitetsnummer()
private fun ObjectNode.periode() : Periode {
    val søknadsperioder = (get("ytelse").get("søknadsperiode")?.let { (it as ArrayNode).map { Periode(it.asText()) } }) ?: emptyList()
    val endringsperioder = (get("ytelse").get("endringsperiode")?.let { (it as ArrayNode).map { Periode(it.asText()) } }) ?: emptyList()
    val perioder = søknadsperioder.plus(endringsperioder)
    return Periode(
        fom = perioder.minByOrNull { it.fom }!!.fom,
        tom = perioder.filter { it.tom != null }.maxByOrNull { it.tom!! }?.tom
    )
}