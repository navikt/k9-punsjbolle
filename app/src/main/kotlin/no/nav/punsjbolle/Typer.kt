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

internal data class JournalpostId(private val value: String) {
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
    internal val brevkode: Brevkode,
    internal val journalpostType: String) {
    PleiepengerSyktBarn("PSB", Brevkode.PLEIEPENGER_BARN_SOKNAD, "PleiepengerSyktBarn"),
    PleiepengerLivetsSluttfase("PPN", Brevkode.SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE, "PleiepengerLivetsSluttfase"),
    Omsorgspenger("OMP", Brevkode.SØKNAD_UTBETALING_OMS, "UtbetaleOmsorgspenger"),
    OmsorgspengerUtbetaling_Korrigering("OMP", Brevkode.FRAVÆRSKORRIGERING_IM_OMS, "UtbetaleOmsorgspenger"),
    OmsorgspengerUtbetaling_Snf("OMP", Brevkode.SØKNAD_UTBETALING_OMS, "UtbetaleOmsorgspenger"),
    OmsorgspengerUtbetaling_Arbeidstaker("OMP", Brevkode.SØKNAD_UTBETALING_OMS_AT, "UtbetaleOmsorgspenger"),
    OmsorgspengerKroniskSyktBarn("OMP_KS", Brevkode.SØKNAD_OMS_UTVIDETRETT_KS, "KroniskSyktBarn"),
    OmsorgspengerAleneOmsorg("OMP_AO", Brevkode.SØKNAD_OMS_UTVIDETRETT_KS, "AleneOmsorg"),
    OmsorgspengerMidlertidigAlene("OMP_MA", Brevkode.SØKNAD_OMS_UTVIDETRETT_MA, "MidlertidigAlene");

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
        internal fun fraBrevkode(brevkode: Brevkode) = when (brevkode) {
            Brevkode.SØKNAD_UTBETALING_OMS -> OmsorgspengerUtbetaling_Snf
            Brevkode.SØKNAD_UTBETALING_OMS_AT -> OmsorgspengerUtbetaling_Arbeidstaker
            Brevkode.PAPIRSØKNAD_UTBETALING_OMS_AT -> OmsorgspengerUtbetaling_Arbeidstaker
            Brevkode.FRAVÆRSKORRIGERING_IM_OMS -> OmsorgspengerUtbetaling_Korrigering
            else -> throw IllegalStateException("Ukjent brevkode $brevkode")
        }
    }
}
