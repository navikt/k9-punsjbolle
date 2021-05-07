package no.nav.punsjbolle.søknad

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.punsjbolle.Identitetsnummer
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.Periode
import no.nav.punsjbolle.Periode.Companion.somPeriode
import no.nav.punsjbolle.Periode.Companion.ÅpenPeriode
import no.nav.punsjbolle.Søknadstype

internal fun ObjectNode.somPunsjetSøknad(
    versjon: String,
    saksnummer: K9Saksnummer?) : PunsjetSøknadMelding.PunsjetSøknad {

    val ytelse = get("ytelse") as ObjectNode
    val søknadstype = Søknadstype.fraK9FormatYtelsetype(ytelse.get("type").asText())

    return when (søknadstype) {
        Søknadstype.PleiepengerSyktBarn -> map(
            søknadstype = søknadstype,
            versjon = versjon,
            saksnummer = saksnummer,
            pleietrengende = barn(),
            periode = pleiepengerSyktBarnPeriode()
        )
        Søknadstype.OmsorgspengerUtbetaling -> map(
            søknadstype = søknadstype,
            versjon = versjon,
            saksnummer = saksnummer,
            periode = omsorgspengerUtbetalingPeriode()
        )
        Søknadstype.OmsorgspengerKroniskSyktBarn -> map(
            søknadstype = søknadstype,
            versjon = versjon,
            saksnummer = saksnummer,
            pleietrengende = barn(),
            periode = ÅpenPeriode
        )
        Søknadstype.OmsorgspengerMidlertidigAlene -> map(
            søknadstype = søknadstype,
            versjon = versjon,
            saksnummer = saksnummer,
            annenPart = annenForelder(),
            periode = omsorgspengerMidlertidigAlenePeriode()
        )
    }
}

private fun ObjectNode.map(
    versjon: String,
    saksnummer: K9Saksnummer?,
    periode: Periode,
    søknadstype: Søknadstype,
    pleietrengende: Identitetsnummer? = null,
    annenPart: Identitetsnummer? = null) : PunsjetSøknadMelding.PunsjetSøknad {
    return PunsjetSøknadMelding.PunsjetSøknad(
        versjon = versjon,
        saksnummer = saksnummer,
        søknadId = søknadsId(),
        journalpostIder = journalpostIder(),
        søknadstype = søknadstype,
        søker = søker(),
        pleietrengende = pleietrengende,
        annenPart = annenPart,
        søknadJson = this,
        periode = periode
    )
}

private fun ObjectNode.søknadsId() = get("søknadId").asText()
private fun ObjectNode.journalpostIder() = (get("journalposter") as ArrayNode).map { it as ObjectNode }.map { it.get("journalpostId").asText().somJournalpostId() }.toSet()
private fun ObjectNode.søker() = get("søker").get("norskIdentitetsnummer").asText().somIdentitetsnummer()
private fun ObjectNode.barn() = get("ytelse").get("barn")?.get("norskIdentitetsnummer")?.asText()?.somIdentitetsnummer()
private fun ObjectNode.annenForelder() = get("ytelse").get("annenForelder").get("norskIdentitetsnummer").asText().somIdentitetsnummer()

private fun ObjectNode.arrayPerioder(navn: String) =
    (get("ytelse").get(navn)?.let { (it as ArrayNode).map { iso8601 -> iso8601.asText().somPeriode() } }) ?: emptyList()
private fun ObjectNode.objectPerioder(navn: String) =
    (get("ytelse").get(navn)?.let { array -> (array as ArrayNode).map { it as ObjectNode }.map { obj -> obj.get("periode").asText().somPeriode() } }) ?: emptyList()

private fun ObjectNode.pleiepengerSyktBarnPeriode() =
    arrayPerioder("søknadsperiode").plus(arrayPerioder("endringsperiode")).somEnPeriode()

private fun ObjectNode.omsorgspengerUtbetalingPeriode() =
    objectPerioder("fraværsperioder").somEnPeriode()

private fun ObjectNode.omsorgspengerMidlertidigAlenePeriode() =
    get("ytelse").get("annenForelder").get("periode")?.asText()?.somPeriode() ?: ÅpenPeriode

private fun List<Periode>.somEnPeriode() : Periode {
    val fraOgMedDatoer = map { it.fom }
    val tilOgMedDatoer = map { it.tom }

    return Periode(
        fom = when (null in fraOgMedDatoer) {
            true -> null
            else -> fraOgMedDatoer.filterNotNull().minByOrNull { it }
        },
        tom = when (null in tilOgMedDatoer) {
            true -> null
            else -> tilOgMedDatoer.filterNotNull().maxByOrNull { it }
        }
    )
}