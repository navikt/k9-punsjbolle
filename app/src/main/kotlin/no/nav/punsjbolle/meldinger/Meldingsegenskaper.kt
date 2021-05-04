package no.nav.punsjbolle

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov

internal interface HentBehov<Behovet> {
    val behovNavn: String
    val mdcPaths: Map<String, String>
    fun validateBehov(packet: JsonMessage)
    fun hentBehov(packet: JsonMessage) : Behovet
}

internal interface LeggTilBehov<BehovInput> {
    fun behov(behovInput: BehovInput) : Behov
}

internal interface HentLøsning<Løsning> {
    val løsningNavn: String
    fun validateLøsning(packet: JsonMessage)
    fun hentLøsning(packet: JsonMessage): Løsning
}