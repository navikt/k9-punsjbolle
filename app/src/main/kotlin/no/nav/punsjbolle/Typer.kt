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
        private fun String.somCorrelationId() = CorrelationId(this)
        internal fun JsonMessage.correlationId() = get(Behovsformat.CorrelationId).asText().somCorrelationId()
    }
}

internal data class Periode(internal val fom: LocalDate, internal val tom: LocalDate?) {
    internal constructor(iso8601: String) : this(
        fom = LocalDate.parse(iso8601.split("/")[0]),
        tom = when (iso8601.split("/")[1] == "..") {
            true -> null
            else -> LocalDate.parse(iso8601.split("/")[1])
        }
    )
    override fun toString() = when (tom) {
        null -> "$fom/.."
        else -> "$fom/$tom"
    }
}

internal enum class Søknadstype(internal val k9SakDto: String) {
    PleiepengerSyktBarn("PSB"),
    OmsorgspengerUtbetaling("OMP"),
    OmsorgspengerKroniskSyktBarn("OMP_KA"),
    OmsorgspengerMidlertidigAlene("OMS_MA");

    internal companion object {
        internal fun fraK9FormatYtelsetype(ytelsetype: String) = when (ytelsetype) {
            "PLEIEPENGER_SYKT_BARN" -> PleiepengerSyktBarn
            else -> throw IllegalStateException("Ukjent ytelsestype $ytelsetype")
        }
    }
}