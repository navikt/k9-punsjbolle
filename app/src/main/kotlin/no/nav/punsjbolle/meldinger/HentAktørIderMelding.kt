package no.nav.punsjbolle.meldinger

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.punsjbolle.AktørId
import no.nav.punsjbolle.AktørId.Companion.somAktørId
import no.nav.punsjbolle.HentLøsning
import no.nav.punsjbolle.Identitetsnummer
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.LeggTilBehov

internal object HentAktørIderMelding : LeggTilBehov<Set<Identitetsnummer>>, HentLøsning<Map<Identitetsnummer, AktørId>> {
    override fun behov(behovInput: Set<Identitetsnummer>): Behov {
        return Behov(løsningNavn, mapOf(
            "måFinneAllePersoner" to true,
            "identitetsnummer" to behovInput,
            "attributter" to setOf(AktørId)
        ))
    }

    override fun validateLøsning(packet: JsonMessage) {
        packet.interestedIn(PersonopplysningerKey)
    }

    override fun hentLøsning(packet: JsonMessage): Map<Identitetsnummer, AktørId> {
        return (packet[PersonopplysningerKey] as ObjectNode)
            .fields()
            .asSequence()
            .map { it.key.somIdentitetsnummer() to (it.value as ObjectNode).get(AktørId).asText().somAktørId() }
            .toMap()
    }

    override val løsningNavn = "HentPersonopplysninger"
    private val PersonopplysningerKey = "@løsninger.$løsningNavn.personopplysninger"
    private const val AktørId = "aktørId"
}