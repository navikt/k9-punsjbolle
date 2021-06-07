package no.nav.punsjbolle.journalpost

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import no.nav.k9.rapid.river.leggTilBehov
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.k9.rapid.river.utenLøsningPåBehov
import no.nav.punsjbolle.meldinger.HentAktørIderMelding
import org.slf4j.LoggerFactory

internal class KopierPunsjbarJournalpostSteg1River(
    rapidsConnection: RapidsConnection
) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(KopierPunsjbarJournalpostSteg1River::class.java),
    mdcPaths = KopierPunsjbarJournalpostMelding.mdcPaths) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(KopierPunsjbarJournalpostMelding.behovNavn)
                it.utenLøsningPåBehov(HentAktørIderMelding.behovNavn)
                KopierPunsjbarJournalpostMelding.validateBehov(it)
            }
        }.register(this)
    }

    override fun doHandlePacket(id: String, packet: JsonMessage): Boolean {
        val kopierPunsjbarJournalpost = KopierPunsjbarJournalpostMelding.hentBehov(packet)
        return (kopierPunsjbarJournalpost.versjon in StøttedeVersjoner).also { støttet -> if (!støttet) {
            logger.warn("Støtter ikke versjon ${kopierPunsjbarJournalpost.versjon}")
        }}
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val kopierPunsjbarJournalpost = KopierPunsjbarJournalpostMelding.hentBehov(packet)

        logger.info("Legger til behov for å hente aktørId på de involverte partene.")
        packet.leggTilBehov(
            KopierPunsjbarJournalpostMelding.behovNavn,
            HentAktørIderMelding.behov(behovInput = kopierPunsjbarJournalpost.identitetsnummer
        ))

        return true
    }

    private companion object {
        val StøttedeVersjoner = setOf("1.0.0")
    }
}