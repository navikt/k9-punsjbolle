package no.nav.punsjbolle

import de.huxhorn.sulky.ulid.ULID
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.testutils.ApplicationContextExtension
import no.nav.punsjbolle.testutils.rapid.mockHentAktørIder
import no.nav.punsjbolle.testutils.rapid.mockKopierJournalpostForK9
import no.nav.punsjbolle.testutils.sisteMeldingSomJSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(ApplicationContextExtension::class)
internal class KopierPunsjbarJournalpostTest(
    private val builder: ApplicationContext.Builder) {

    private val rapid = TestRapid().also {
        it.registerApplicationContext(builder.build())
    }

    @Test
    fun `kopiere en punsjbar journalpost for pleiepenger sykt barn`() {
        rapid.sendTestMessage(kopierPunsjbarJournalpost())
        rapid.mockHentAktørIder(setOf(fra, til, pleietrengende))
        rapid.mockKopierJournalpostForK9(kopiertJournalpostId)
        val journalpostId =
            rapid.sisteMeldingSomJSONObject().getJSONObject("@løsninger").getJSONObject("KopierPunsjbarJournalpost").getString("journalpostId").somJournalpostId()
        assertEquals(kopiertJournalpostId, journalpostId)
    }


    private companion object {
        private val fra = "22222222222".somIdentitetsnummer()
        private val til = "33333333333".somIdentitetsnummer()
        private val pleietrengende = "44444444444".somIdentitetsnummer()
        private val kopiertJournalpostId = "999999999".somJournalpostId()

        private val ulid = ULID()

        fun kopierPunsjbarJournalpost() = Behovssekvens(
            id = ulid.nextULID(),
            correlationId = "${UUID.randomUUID()}",
            behov = arrayOf(
                Behov(
                    navn = "KopierPunsjbarJournalpost",
                    input = mapOf(
                        "versjon" to "1.0.0",
                        "journalpostId" to "11111111111",
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