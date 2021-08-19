package no.nav.punsjbolle

import de.huxhorn.sulky.ulid.ULID
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.joark.Journalpost
import no.nav.punsjbolle.joark.SafClient
import no.nav.punsjbolle.testutils.ApplicationContextExtension
import no.nav.punsjbolle.testutils.rapid.mockHentAktørIder
import no.nav.punsjbolle.testutils.rapid.mockKopierJournalpostForK9
import no.nav.punsjbolle.testutils.rapid.sisteMeldingErKlarForArkivering
import no.nav.punsjbolle.testutils.rapid.sisteMeldingSomJSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.util.*

@ExtendWith(ApplicationContextExtension::class)
internal class KopierPunsjbarJournalpostTest(
    private val builder: ApplicationContext.Builder) {

    private val safClientMock = mockk<SafClient>().also { mock ->
        coEvery { mock.hentJournalpost(mottattInngåendeJournalpost, any()) }.returns(mottattInngåendeJournalpost.somTestJournalpost("MOTTATT", "I"))
        coEvery { mock.hentJournalpost(journalførtInngåendeJournalpost, any()) }.returns(journalførtInngåendeJournalpost.somTestJournalpost("JOURNALFOERT", "I"))
        coEvery { mock.hentJournalpost(ferdigstiltNotat, any()) }.returns(mottattInngåendeJournalpost.somTestJournalpost("FERDIGSTILT", "N"))
    }

    private val rapid = TestRapid().also {
        it.registerApplicationContext(builder.also { builder ->
            builder.safClient = safClientMock
        }.build())
    }

    @Test
    fun `kopiere en mottatt inngående journalpost`() {
        rapid.sendTestMessage(kopierPunsjbarJournalpost(
            journalpostId = mottattInngåendeJournalpost
        ))
        rapid.mockHentAktørIder(setOf(defaultFra, defaultTil, pleietrengende))
        rapid.mockKopierJournalpostForK9(kopiertJournalpostId)
        assertEquals(kopiertJournalpostId, rapid.kopiertJournalpostId())
        rapid.sisteMeldingErKlarForArkivering()
    }

    @Test
    fun `kopiere en mottatt inngående journalpost til seg selv`() {
        rapid.sendTestMessage(kopierPunsjbarJournalpost(
            fra = defaultFra,
            til = defaultFra,
            journalpostId = mottattInngåendeJournalpost
        ))
        rapid.mockHentAktørIder(setOf(defaultFra, pleietrengende))
        rapid.mockKopierJournalpostForK9(kopiertJournalpostId)
        assertEquals(kopiertJournalpostId, rapid.kopiertJournalpostId())
        rapid.sisteMeldingErKlarForArkivering()
    }

    @Test
    fun `kopiere en journalført inngående journalpost`() {
        rapid.sendTestMessage(kopierPunsjbarJournalpost(
            journalpostId = journalførtInngåendeJournalpost
        ))
        rapid.mockHentAktørIder(setOf(defaultFra, defaultTil, pleietrengende))
        rapid.mockKopierJournalpostForK9(kopiertJournalpostId)
        assertEquals(kopiertJournalpostId, rapid.kopiertJournalpostId())
        rapid.sisteMeldingErKlarForArkivering()
    }

    @Test
    fun `kopiere et ferdigstilt notat`() {
        rapid.sendTestMessage(kopierPunsjbarJournalpost(
            journalpostId = ferdigstiltNotat
        ))
        rapid.mockHentAktørIder(setOf(defaultFra, defaultTil, pleietrengende))
        rapid.mockKopierJournalpostForK9(kopiertJournalpostId)
        assertEquals(kopiertJournalpostId, rapid.kopiertJournalpostId())
        rapid.sisteMeldingErKlarForArkivering()
    }


    private companion object {
        private val defaultFra = "22222222222".somIdentitetsnummer()
        private val defaultTil = "33333333333".somIdentitetsnummer()
        private val pleietrengende = "44444444444".somIdentitetsnummer()
        private val kopiertJournalpostId = "999999999".somJournalpostId()

        private val mottattInngåendeJournalpost = "11111111111".somJournalpostId()
        private val journalførtInngåendeJournalpost = "11111111112".somJournalpostId()
        private val ferdigstiltNotat = "11111111113".somJournalpostId()
        private fun JournalpostId.somTestJournalpost(
            journalpoststatus: String,
            journalposttype: String
        ) = Journalpost(
            journalpostId = this,
            brevkode = null,
            opprettet = LocalDateTime.now(),
            eksternReferanse = null,
            sak = null,
            journalpoststatus = journalpoststatus,
            journalposttype = journalposttype
        )

        private fun TestRapid.kopiertJournalpostId() =
            sisteMeldingSomJSONObject().getJSONObject("@løsninger").getJSONObject("KopierPunsjbarJournalpost").getString("journalpostId").somJournalpostId()

        private val ulid = ULID()

        fun kopierPunsjbarJournalpost(
            fra: Identitetsnummer = defaultFra,
            til: Identitetsnummer = defaultTil,
            journalpostId: JournalpostId
        ) = Behovssekvens(
            id = ulid.nextULID(),
            correlationId = "${UUID.randomUUID()}",
            behov = arrayOf(
                Behov(
                    navn = "KopierPunsjbarJournalpost",
                    input = mapOf(
                        "versjon" to "1.0.0",
                        "journalpostId" to "$journalpostId",
                        "fra" to "$fra",
                        "til" to "$til",
                        "pleietrengende" to "$pleietrengende",
                        "søknadstype" to "PleiepengerSyktBarn"
                    )
                )
            )
        ).keyValue.second
    }
}