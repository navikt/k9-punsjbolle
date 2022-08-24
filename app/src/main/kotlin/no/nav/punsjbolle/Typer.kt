package no.nav.punsjbolle

import no.nav.helse.rapids_rivers.JsonMessage
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
    internal fun erÅpenPeriode() = this == ÅpenPeriode

    internal companion object {
        private const val Åpen = ".."

        private fun LocalDate?.iso8601() = when (this) {
            null -> Åpen
            else -> "$this"
        }

        internal val ÅpenPeriode = Periode(null,null)
        internal fun String.somPeriode() : Periode {
            val split = split("/")
            require(split.size == 2) { "Ugylig periode $this."}
            return Periode(
                fom = when (split[0]) {
                    Åpen -> null
                    else -> LocalDate.parse(split[0])
                },
                tom = when (split[1]) {
                    Åpen -> null
                    else -> LocalDate.parse(split[1])
                }
            )
        }
        internal fun LocalDate.somPeriode() = Periode(fom = this, tom = this)
        internal fun Periode.forsikreLukketPeriode() = when {
            fom != null && tom != null -> this
            fom != null -> fom.somPeriode()
            tom != null -> tom.somPeriode()
            else -> throw IllegalStateException("Må være satt minst fom eller tom for å lage en lukket periode.")
        }
    }
}

internal enum class Søknadstype(
    internal val k9YtelseType: String,
    internal val k9Type: String,
    internal val journalpostType: String) {
    PleiepengerSyktBarn("PSB", "PLEIEPENGER_SOKNAD", "PleiepengerSyktBarn"),
    PleiepengerLivetsSluttfase("PPN", "PLEIEPENGER_LIVETS_SLUTTFASE_SOKNAD", "PleiepengerLivetsSluttfase"),
    Omsorgspenger("OMP", "SØKNAD_UTBETALING_OMS", "UtbetaleOmsorgspenger"),
    OmsorgspengerUtbetaling_Korrigering("OMP", "FRAVÆRSKORRIGERING_IM_OMS", "UtbetaleOmsorgspenger"),
    OmsorgspengerKroniskSyktBarn("OMP_KS", "SØKNAD_OMS_UTVIDETRETT_KS", "KroniskSyktBarn"),
    OmsorgspengerAleneOmsorg("OMP_AO", "SØKNAD_OMS_UTVIDETRETT_AO", "AleneOmsorg"),
    OmsorgspengerMidlertidigAlene("OMP_MA", "SØKNAD_OMS_UTVIDETRETT_MA", "MidlertidigAlene");

    internal companion object {
        internal fun fraK9FormatYtelsetype(ytelsetype: String) = when (ytelsetype) {
            "PLEIEPENGER_SYKT_BARN" -> PleiepengerSyktBarn
            "PLEIEPENGER_LIVETS_SLUTTFASE" -> PleiepengerLivetsSluttfase
            "OMP_UT" -> OmsorgspengerUtbetaling_Korrigering
            "OMP_UTV_KS" -> OmsorgspengerKroniskSyktBarn
            "OMP_UTV_MA" -> OmsorgspengerMidlertidigAlene
            "OMP_UTV_AO" -> OmsorgspengerAleneOmsorg
            "OMP" -> Omsorgspenger
            else -> throw IllegalStateException("Ukjent ytelsestype $ytelsetype")
        }
    }
}
