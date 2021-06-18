package no.nav.punsjbolle.journalpost

import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.punsjbolle.CorrelationId.Companion.correlationId
import no.nav.punsjbolle.Periode.Companion.somPeriode
import no.nav.punsjbolle.joark.Journalpost.Companion.kanKopieres
import no.nav.punsjbolle.joark.SafClient
import no.nav.punsjbolle.k9sak.K9SakClient
import no.nav.punsjbolle.meldinger.HentAktørIderMelding
import no.nav.punsjbolle.meldinger.HentK9SaksnummerMelding
import no.nav.punsjbolle.meldinger.KopierJournalpostForK9Melding
import no.nav.punsjbolle.ruting.RutingService
import org.slf4j.LoggerFactory

internal class KopierPunsjbarJournalpostSteg2River(
    rapidsConnection: RapidsConnection,
    private val rutingService: RutingService,
    private val k9SakClient: K9SakClient,
    private val safClient: SafClient
) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(KopierPunsjbarJournalpostSteg2River::class.java),
    mdcPaths = KopierPunsjbarJournalpostMelding.mdcPaths) {

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(KopierPunsjbarJournalpostMelding.behovNavn)
                it.harLøsningPåBehov(HentAktørIderMelding.behovNavn)
                it.utenLøsningPåBehov(KopierJournalpostForK9Melding.behovNavn)
                KopierPunsjbarJournalpostMelding.validateBehov(it)
                HentAktørIderMelding.validateLøsning(it)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val kopierPunsjbarJournalpost = KopierPunsjbarJournalpostMelding.hentBehov(packet)
        val aktørIder = HentAktørIderMelding.hentLøsning(packet)
        val correlationId = packet.correlationId()

        val journalpost = runBlocking { safClient.hentJournalpost(
            journalpostId = kopierPunsjbarJournalpost.journalpostId,
            correlationId = correlationId
        )}

        val periode = journalpost.opprettet.toLocalDate().somPeriode()

        val destinasjon = runBlocking { rutingService.destinasjon(
            søker = kopierPunsjbarJournalpost.fra,
            pleietrengende = kopierPunsjbarJournalpost.pleietrengende,
            annenPart = kopierPunsjbarJournalpost.til,
            søknadstype = kopierPunsjbarJournalpost.søknadstype,
            aktørIder = aktørIder.values.toSet(),
            fraOgMed = periode.fom!!,
            correlationId = correlationId
        )}

        require(RutingService.Destinasjon.K9Sak == destinasjon) {
            "Kan ikke kopiere journalpost. Partene har Destinasjon=[$destinasjon]"
        }

        val fraSaksnummerGrunnlag = HentK9SaksnummerMelding.HentK9SaksnummerGrunnlag(
            søknadstype = kopierPunsjbarJournalpost.søknadstype,
            søker = aktørIder.getValue(kopierPunsjbarJournalpost.fra),
            pleietrengende = kopierPunsjbarJournalpost.pleietrengende?.let { aktørIder.getValue(it) },
            periode = periode,
            annenPart = kopierPunsjbarJournalpost.annenPart?.let { aktørIder.getValue(it) }
        )

        logger.info("Validerer at journalposten kan kopieres")
        val kanKopieres = runBlocking { journalpost.kanKopieres {
            k9SakClient.hentEksisterendeSaksnummer(
                grunnlag = fraSaksnummerGrunnlag,
                correlationId = correlationId
            )
        }}
        require(kanKopieres) { "Kan ikke kopieres. $journalpost." }

        logger.info("Henter/Oppretter saksnummer for personen det kopieres fra, og personen det kopieres til.")
        val fraSaksnummer = runBlocking { k9SakClient.hentEllerOpprettSaksnummer(
            grunnlag = fraSaksnummerGrunnlag,
            correlationId = correlationId
        )}

        val tilSaksnummer = runBlocking { k9SakClient.hentEllerOpprettSaksnummer(
            grunnlag = fraSaksnummerGrunnlag.copy(
                søker = aktørIder.getValue(kopierPunsjbarJournalpost.til)
            ),
            correlationId = correlationId
        )}

        logger.info("Legger til behov for å kopiere journalpost")
        packet.leggTilBehov(
            KopierPunsjbarJournalpostMelding.behovNavn,
            KopierJournalpostForK9Melding.behov(behovInput = KopierJournalpostForK9Melding.KopierJournalpostForK9(
                fra = KopierJournalpostForK9Melding.Part(identitetsnummer = kopierPunsjbarJournalpost.fra, saksnummer = fraSaksnummer),
                til = KopierJournalpostForK9Melding.Part(identitetsnummer = kopierPunsjbarJournalpost.til, saksnummer = tilSaksnummer),
                journalpostId = kopierPunsjbarJournalpost.journalpostId
            ))
        )
        return true
    }
}