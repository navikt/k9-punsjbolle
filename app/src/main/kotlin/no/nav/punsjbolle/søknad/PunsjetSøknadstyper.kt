package no.nav.punsjbolle.søknad

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.punsjbolle.Identitetsnummer
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.Periode
import no.nav.punsjbolle.Periode.Companion.somPeriode
import no.nav.punsjbolle.Periode.Companion.ÅpenPeriode
import no.nav.punsjbolle.Søknadstype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime


internal val logger: Logger = LoggerFactory.getLogger("no.nav.punsjbolle.søknad.PunsjetSøknadstyperKt.søknadstype")
internal fun ObjectNode.søknadstype(brevkode: String? = null): Søknadstype {
    val ytelsetype = (get("ytelse") as ObjectNode).get("type").asText()
    logger.info("Debugger: ytelse=[$ytelsetype], brevkode=[$brevkode]")
    return if (ytelsetype == "OMP_UT" && !brevkode.isNullOrBlank()) {
        logger.info("Utleder søknadstype fra brevkode...")
        val gyldigBrevkode = Brevkode.registrerteKoder()
            .map { it.value }
            .firstOrNull { it.offisiellKode == brevkode }
            ?: throw IllegalStateException("Ikke mulig å utlede søknadstype for ytelse=[$ytelsetype] og brevkode=[$brevkode]")
        Søknadstype.fraBrevkode(gyldigBrevkode)
    } else {
        logger.info("Utleder søknadstype fra ytelsestype...")
        return Søknadstype.fraK9FormatYtelsetype(ytelsetype)
    }
}

internal fun ObjectNode.periode(søknadstype: Søknadstype) = when (søknadstype) {
    Søknadstype.PleiepengerSyktBarn -> pleiepengerSyktBarnPeriode(mottatt = mottatt())
    Søknadstype.PleiepengerLivetsSluttfase -> omsorgspengerKroniskSyktBarnPeriode(mottatt = mottatt())
    Søknadstype.OmsorgspengerKroniskSyktBarn -> omsorgspengerKroniskSyktBarnPeriode(mottatt = mottatt())
    Søknadstype.OmsorgspengerMidlertidigAlene -> omsorgspengerMidlertidigAlenePeriode()
    Søknadstype.OmsorgspengerAleneOmsorg -> omsorgspengerAleneOmsorgPeriode()
    Søknadstype.OmsorgspengerUtbetaling_Korrigering -> korrigeringAvInntektsmelding()
    Søknadstype.OmsorgspengerUtbetaling_Snf, Søknadstype.OmsorgspengerUtbetaling_Arbeidstaker -> omsorgspengerutbetaling()
    Søknadstype.Omsorgspenger -> ÅpenPeriode
}

internal fun ObjectNode.somPunsjetSøknad(
    versjon: String,
    saksbehandler: String,
    saksnummer: K9Saksnummer?,
    brevkode: String?
): PunsjetSøknadMelding.PunsjetSøknad {

    val søknadstype = søknadstype(brevkode)
    val periode = periode(søknadstype = søknadstype)

    return when (søknadstype) {
        Søknadstype.PleiepengerSyktBarn, Søknadstype.OmsorgspengerKroniskSyktBarn, Søknadstype.OmsorgspengerAleneOmsorg, Søknadstype.Omsorgspenger -> map(
            søknadstype = søknadstype,
            versjon = versjon,
            saksnummer = saksnummer,
            pleietrengende = barn(),
            periode = periode,
            saksbehandler = saksbehandler
        )
        Søknadstype.PleiepengerLivetsSluttfase -> map(
            søknadstype = søknadstype,
            versjon = versjon,
            saksnummer = saksnummer,
            pleietrengende = pleietrengende(),
            periode = periode,
            saksbehandler = saksbehandler
        )
        Søknadstype.OmsorgspengerUtbetaling_Korrigering, Søknadstype.OmsorgspengerUtbetaling_Snf, Søknadstype.OmsorgspengerUtbetaling_Arbeidstaker -> map(
            søknadstype = søknadstype,
            versjon = versjon,
            saksnummer = saksnummer,
            periode = periode,
            saksbehandler = saksbehandler
        )
        Søknadstype.OmsorgspengerMidlertidigAlene -> map(
            søknadstype = søknadstype,
            versjon = versjon,
            saksnummer = saksnummer,
            annenPart = annenForelder(),
            periode = periode,
            saksbehandler = saksbehandler
        )
    }
}

private fun ObjectNode.map(
    versjon: String,
    saksbehandler: String,
    saksnummer: K9Saksnummer?,
    periode: Periode,
    søknadstype: Søknadstype,
    pleietrengende: Identitetsnummer? = null,
    annenPart: Identitetsnummer? = null
): PunsjetSøknadMelding.PunsjetSøknad {
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
        periode = periode,
        mottatt = mottatt(),
        saksbehandler = saksbehandler
    )
}

private fun ObjectNode.søknadsId() = get("søknadId").asText()
private fun ObjectNode.mottatt() = ZonedDateTime.parse(get("mottattDato").asText())
private fun ObjectNode.journalpostIder() = get("journalposter")?.let {
    (it as ArrayNode).map { it as ObjectNode }.map { it.get("journalpostId").asText().somJournalpostId() }.toSet()
} ?: emptySet()

private fun ObjectNode.søker() = get("søker").get("norskIdentitetsnummer").asText().somIdentitetsnummer()
private fun ObjectNode.barn() = get("ytelse").get("barn")?.get("norskIdentitetsnummer")?.asText()?.somIdentitetsnummer()
private fun ObjectNode.pleietrengende() =
    get("ytelse").get("pleietrengende")?.get("norskIdentitetsnummer")?.asText()?.somIdentitetsnummer()

private fun ObjectNode.annenForelder() =
    get("ytelse").get("annenForelder").get("norskIdentitetsnummer").asText().somIdentitetsnummer()

private fun ObjectNode.arrayPerioder(navn: String) =
    (get("ytelse").get(navn)?.let { (it as ArrayNode).map { iso8601 -> iso8601.asText().somPeriode() } }) ?: emptyList()

private fun ObjectNode.objectPerioder(navn: String) =
    (get("ytelse").get(navn)?.let { array ->
        (array as ArrayNode).map { it as ObjectNode }.map { obj -> obj.get("periode").asText().somPeriode() }
    }) ?: emptyList()

private fun ObjectNode.pleiepengerSyktBarnPeriode(mottatt: ZonedDateTime) =
    arrayPerioder("søknadsperiode")
        .plus(arrayPerioder("endringsperiode"))
        .plus(arrayPerioder("trekkKravPerioder"))
        .somEnPeriode()
        .let { periode ->
            when (periode.erÅpenPeriode()) {
                true -> Periode(fom = mottatt.toLocalDate(), tom = null)
                false -> periode
            }
        }

private fun ObjectNode.omsorgspengerKroniskSyktBarnPeriode(mottatt: ZonedDateTime) =
    arrayPerioder("søknadsperiode")
        .somEnPeriode()
        .let { periode ->
            when (periode.erÅpenPeriode()) {
                true -> Periode(fom = mottatt.toLocalDate(), tom = null)
                false -> periode
            }
        }

private fun ObjectNode.korrigeringAvInntektsmelding() =
    objectPerioder("fraværsperioderKorrigeringIm").somEnPeriode()

private fun ObjectNode.omsorgspengerutbetaling() =
    objectPerioder("fraværsperioder").somEnPeriode()

private fun ObjectNode.omsorgspengerMidlertidigAlenePeriode() =
    get("ytelse").get("annenForelder").get("periode")?.asText()?.somPeriode() ?: ÅpenPeriode

private fun ObjectNode.omsorgspengerAleneOmsorgPeriode() =
    get("ytelse").get("periode")?.asText()?.somPeriode() ?: ÅpenPeriode

private fun List<Periode>.somEnPeriode(): Periode {
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
