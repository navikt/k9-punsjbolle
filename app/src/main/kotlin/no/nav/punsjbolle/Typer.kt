package no.nav.punsjbolle

import io.ktor.application.*
import io.ktor.features.*
import no.nav.k9.søknad.ytelse.Ytelse
import java.util.*

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
        internal fun genererCorrelationId() = CorrelationId("omsorgsdager-${UUID.randomUUID()}")
        internal fun String.somCorrelationId() = CorrelationId(this)
        internal fun ApplicationCall.correlationId() = requireNotNull(callId).somCorrelationId()
    }
}

internal enum class Søknadstype(internal val k9SakDto: String) {
    PleiepengerSyktBarn("PSB"),
    OmsorgspengerUtbetaling("OMP"),
    OmsorgspengerKroniskSyktBarn("OMP_KA"),
    OmsorgspengerMidlertidigAlene("OMS_MA");

    internal companion object {
        internal fun fraK9FormatYtelse(ytelse: Ytelse) = when (ytelse) {
            is no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn -> PleiepengerSyktBarn
            is no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling -> OmsorgspengerUtbetaling
            is no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerKroniskSyktBarn -> OmsorgspengerKroniskSyktBarn
            is no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerMidlertidigAlene -> OmsorgspengerMidlertidigAlene
            else -> throw IllegalStateException("Ukjent ytelsestype ${ytelse.javaClass}")
        }
    }
}