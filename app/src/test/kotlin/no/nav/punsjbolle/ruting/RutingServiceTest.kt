package no.nav.punsjbolle.ruting

import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.punsjbolle.AktørId.Companion.somAktørId
import no.nav.punsjbolle.CorrelationId.Companion.somCorrelationId
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.Søknadstype
import no.nav.punsjbolle.k9sak.K9SakClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException
import java.time.LocalDate
import java.util.*

internal class RutingServiceTest {

    private val journalpostOverstyrtTilK9Sak1 = "789789788".somJournalpostId()
    private val journalpostOverstyrtTilK9Sak2 = "789789789".somJournalpostId()
    private val journalpostIkkeOverstyrtTilK9Sak = "894894894".somJournalpostId()
    private val aktørIdIUnntaksliste = "11111111111".somAktørId()
    private val aktørIdIkkeIUnntaksliste = "2222222222".somAktørId()

    private val k9SakClientMock = mockk<K9SakClient>().also {
        coEvery { it.inngårIUnntaksliste(setOf(aktørIdIUnntaksliste), Søknadstype.PleiepengerSyktBarn, any()) }.returns(true)
        coEvery { it.inngårIUnntaksliste(setOf(aktørIdIkkeIUnntaksliste), Søknadstype.PleiepengerSyktBarn, any()) }.returns(false)
        coEvery { it.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any(), any()) }.returns(
            RutingGrunnlag(
            søker = true
        )
        )
    }

    private val rutingService = RutingService(
        k9SakClient = k9SakClientMock,
        infotrygdClient = mockk(),
        overstyrTilK9SakJournalpostIds = setOf(
            journalpostOverstyrtTilK9Sak1,
            journalpostOverstyrtTilK9Sak2
        )
    )


    @Test
    fun `I unntaksliste og overstyrt til K9Sak skal rutes til Infotrygd`() {
        assertEquals(RutingService.Destinasjon.Infotrygd, hentDestinasjon(
            journalpostIds = setOf(journalpostOverstyrtTilK9Sak1, journalpostOverstyrtTilK9Sak2),
            iUnntaksliste = true
        ))
    }

    @Test
    fun `Ikke i unntaksliste og overstyrt til K9Sak skal rutes til K9Sak`() {
        assertEquals(RutingService.Destinasjon.K9Sak, hentDestinasjon(
            journalpostIds = setOf(journalpostOverstyrtTilK9Sak1, journalpostOverstyrtTilK9Sak2),
            iUnntaksliste = false
        ))
    }

    @Test
    fun `Feiler om et subset av journalpostene er overstyrt til K9Sak`() {
        assertThrows<IllegalStateException> {
            hentDestinasjon(
                journalpostIds = setOf(journalpostOverstyrtTilK9Sak1, journalpostIkkeOverstyrtTilK9Sak),
                iUnntaksliste = false
            )
        }
    }

    @Test
    fun `Ikke i unntaksliste eller overstyrt til K9Sak gir normal ruting`() {
        coVerify { k9SakClientMock.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any(), any()) wasNot Called }
        assertEquals(RutingService.Destinasjon.K9Sak, hentDestinasjon(
            journalpostIds = setOf(journalpostIkkeOverstyrtTilK9Sak),
            iUnntaksliste = false
        ))
        coVerify(exactly = 1) { k9SakClientMock.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any(), any()) }
    }

    private fun hentDestinasjon(
        journalpostIds: Set<JournalpostId>,
        iUnntaksliste: Boolean
    ) = runBlocking { rutingService.destinasjon(
        søker = "12345678911".somIdentitetsnummer(),
        fraOgMed = LocalDate.now(),
        søknadstype = Søknadstype.PleiepengerSyktBarn,
        aktørIder = when (iUnntaksliste) {
            true -> setOf(aktørIdIUnntaksliste)
            false -> setOf(aktørIdIkkeIUnntaksliste)
        },
        journalpostIds = journalpostIds,
        correlationId = "${UUID.randomUUID()}".somCorrelationId()
    )}
}