package no.nav.punsjbolle

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov

internal interface HentBehov<Behovet> {
    val mdcPaths: Map<String, String>
    fun validateBehov(packet: JsonMessage)
    fun hentBehov(packet: JsonMessage) : Behovet
}

internal interface LeggTilBehov<BehovInput> {
    fun behov(behovInput: BehovInput) : Behov
}

internal interface HentLøsning<Løsning> {
    fun validateLøsning(packet: JsonMessage)
    fun hentLøsning(packet: JsonMessage): Løsning
}

internal interface BehovMedLøsning<BehovInput, Løsning> {
    fun behovMedLøsning(behovInput: BehovInput, løsning: Løsning) : Pair<Behov, Map<String,*>>
}