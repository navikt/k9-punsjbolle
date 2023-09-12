package no.nav.punsjbolle

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9.rapid.behov.Behovsformat
import java.time.LocalDate

internal data class Identitetsnummer private constructor(private val value: String) {
    init { require(value.matches(Regex)) { "Ugyldig identitetsnummer" } }
    override fun toString() = value
    internal companion object {
        private val Regex = "\\d{11,25}".toRegex()
        internal fun String.somIdentitetsnummer() = Identitetsnummer(this)
    }
}

internal data class AktørId private constructor(private val value: String) {
    init { require(value.matches(Regex)) { "$value er en ugyldig aktørId" } }
    override fun toString() = value
    internal companion object {
        private val Regex = "\\d{5,40}".toRegex()
        internal fun String.somAktørId() = AktørId(this)
    }
}

internal data class K9Saksnummer private constructor(private val value: String) {
    init { require(value.matches(Regex)) { "$value er et ugyldig K9 saksnummer" } }
    override fun toString() = value
    internal companion object {
        private val Regex = "[A-Za-z0-9]{5,20}".toRegex()
        internal fun String.somK9Saksnummer() = K9Saksnummer(this)
    }
}

internal data class JournalpostId private constructor(private val value: String) {
    init { require(value.matches(Regex)) { "$value er en ugylidig journalpostId" } }
    override fun toString() = value
    internal companion object {
        private val Regex = "\\d{5,40}".toRegex()
        internal fun String.somJournalpostId() = JournalpostId(this)
    }
}

internal data class CorrelationId private constructor(private val value: String) {
    init { require(value.matches(Regex)) { "$value er en ugyldig correlation id" } }
    override fun toString() = value
    internal companion object {
        private val Regex = "[a-zA-Z0-9_.\\-æøåÆØÅ]{5,200}".toRegex()
        internal fun String.somCorrelationId() = CorrelationId(this)
        internal fun JsonMessage.correlationId() = get(Behovsformat.CorrelationId).asText().somCorrelationId()
    }
}

internal data class Periode(internal val fom: LocalDate?, internal val tom: LocalDate?) {
    override fun toString() = "${fom.iso8601()}/${tom.iso8601()}"

    internal companion object {
        private const val Åpen = ".."

        private fun LocalDate?.iso8601() = when (this) {
            null -> Åpen
            else -> "$this"
        }

        internal fun LocalDate.somPeriode() = Periode(fom = this, tom = this)
    }
}

internal enum class Søknadstype(
    internal val k9YtelseType: String,
    internal val brevkode: Brevkode,
    internal val journalpostType: String) {
    PleiepengerSyktBarn("PSB", Brevkode.PLEIEPENGER_BARN_SOKNAD, "PleiepengerSyktBarn"),
    PleiepengerLivetsSluttfase("PPN", Brevkode.SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE, "PleiepengerLivetsSluttfase"),
    Omsorgspenger("OMP", Brevkode.SØKNAD_UTBETALING_OMS, "UtbetaleOmsorgspenger"),
    OmsorgspengerUtbetaling_Arbeidstaker("OMP", Brevkode.SØKNAD_UTBETALING_OMS_AT, "UtbetaleOmsorgspenger"),
    OmsorgspengerUtbetaling_Papirsøknad_Arbeidstaker("OMP", Brevkode.PAPIRSØKNAD_UTBETALING_OMS_AT, "UtbetaleOmsorgspenger"),
    OmsorgspengerUtbetaling_Korrigering("OMP", Brevkode.FRAVÆRSKORRIGERING_IM_OMS, "UtbetaleOmsorgspenger"),
    OmsorgspengerKroniskSyktBarn("OMP_KS", Brevkode.SØKNAD_OMS_UTVIDETRETT_KS, "KroniskSyktBarn"),
    OmsorgspengerAleneOmsorg("OMP_AO", Brevkode.SØKNAD_OMS_UTVIDETRETT_AO, "AleneOmsorg"),
    Opplæringspenger("OLP", Brevkode.OPPLÆRINGSPENGER_SOKNAD, "Opplæringspenger"),
    OmsorgspengerMidlertidigAlene("OMP_MA", Brevkode.SØKNAD_OMS_UTVIDETRETT_MA, "MidlertidigAlene");
}
