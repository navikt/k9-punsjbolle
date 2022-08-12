package no.nav.punsjbolle.journalpost

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.punsjbolle.meldinger.HentAktørIderMelding
import no.nav.punsjbolle.meldinger.KopierJournalpostForK9Melding
import org.slf4j.LoggerFactory

internal class KopierPunsjbarJournalpostSteg3River(
    rapidsConnection: RapidsConnection,
    private val punsjbarJournalpostClient: PunsjbarJournalpostClient
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
        val nyJournalpostId = KopierJournalpostForK9Melding.hentLøsning(packet)
        val kopiertPunsjbarJournalpost = KopierPunsjbarJournalpostMelding.hentBehov(packet, null)
        val opprinneligJournalpostId = kopiertPunsjbarJournalpost.journalpostId
        val aktørId = HentAktørIderMelding.hentLøsning(packet).getValue(kopiertPunsjbarJournalpost.til)

        packet.leggTilLøsning(
            behov = KopierPunsjbarJournalpostMelding.behovNavn,
            løsning = mapOf(
                "journalpostId" to "$nyJournalpostId"
            )
        )

        punsjbarJournalpostClient.send(KopiertJournalpost(
            journalpostId = nyJournalpostId,
            aktørId = aktørId,
            søknadstype = kopiertPunsjbarJournalpost.søknadstype,
            opprinneligJournalpostId = kopiertPunsjbarJournalpost.journalpostId
        ))

        logger.info("Behov ${KopierPunsjbarJournalpostMelding.behovNavn} løst. OpprinneligJournalpostId=[${opprinneligJournalpostId}], NyJournalpostId=[$nyJournalpostId]")

        return true
    }
}
