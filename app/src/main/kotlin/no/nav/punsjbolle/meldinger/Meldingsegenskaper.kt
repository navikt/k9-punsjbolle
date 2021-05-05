package no.nav.punsjbolle

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.river.leggTilLøsning

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

internal interface LeggTilLøsning<Løsning> {
    fun løsning(løsning: Løsning) : Pair<String, Map<String,*>>
}

internal fun JsonMessage.leggTilLøsningPar(pair: Pair<String, Map<String,*>>) =
    leggTilLøsning(behov = pair.first, løsning = pair.second)


internal interface BehovMedLøsning<BehovInput, Løsning> {
    fun behovMedLøsning(behovInput: BehovInput, løsning: Løsning) : Pair<Behov, Map<String,*>>
}