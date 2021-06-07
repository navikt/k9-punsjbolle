package no.nav.punsjbolle.journalpost

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.punsjbolle.meldinger.HentAktørIderMelding
import no.nav.punsjbolle.meldinger.KopierJournalpostForK9Melding
import org.slf4j.LoggerFactory

internal class KopierPunsjbarJournalpostSteg3River(
    rapidsConnection: RapidsConnection
) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(KopierPunsjbarJournalpostSteg3River::class.java),
    mdcPaths = KopierPunsjbarJournalpostMelding.mdcPaths) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(KopierPunsjbarJournalpostMelding.behovNavn)
                it.harLøsningPåBehov(HentAktørIderMelding.behovNavn)
                it.harLøsningPåBehov(KopierJournalpostForK9Melding.behovNavn)
                KopierPunsjbarJournalpostMelding.validateBehov(it)
                KopierJournalpostForK9Melding.validateLøsning(it)
                HentAktørIderMelding.validateLøsning(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val kopiertJournalpostId = KopierJournalpostForK9Melding.hentLøsning(packet)
        val aktørId = HentAktørIderMelding.hentLøsning(packet).getValue(KopierPunsjbarJournalpostMelding.hentBehov(packet).til)

        logger.info("Behov ${KopierPunsjbarJournalpostMelding.behovNavn} løst med JournalpostId=[$kopiertJournalpostId]")

        packet.leggTilLøsning(
            behov = KopierPunsjbarJournalpostMelding.behovNavn,
            løsning = mapOf(
                "journalpostId" to kopiertJournalpostId
            )
        )

        logger.warn("TODO: Sende på topic til Punsj for å opprette oppgave")

        return true
    }
}