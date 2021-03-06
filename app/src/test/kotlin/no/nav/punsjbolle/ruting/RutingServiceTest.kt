package no.nav.punsjbolle.ruting

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.punsjbolle.AktørId.Companion.somAktørId
import no.nav.punsjbolle.CorrelationId.Companion.somCorrelationId
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.Søknadstype
import no.nav.punsjbolle.infotrygd.InfotrygdClient
import no.nav.punsjbolle.k9sak.K9SakClient
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import java.lang.IllegalStateException
import java.time.LocalDate
import java.util.*

internal class RutingServiceTest {

    private val journalpostOverstyrtTilK9Sak1 = "789789788".somJournalpostId()
    private val journalpostOverstyrtTilK9Sak2 = "789789789".somJournalpostId()
    private val journalpostIkkeOverstyrtTilK9Sak = "894894894".somJournalpostId()
    private val aktørIdIUnntaksliste = "11111111111".somAktørId()
    private val aktørIdIkkeIUnntaksliste = "2222222222".somAktørId()
    private val k9SakClientMock = mockk<K9SakClient>()
    private val infotrygdClientMock = mockk<InfotrygdClient>()
    private val rutingService = RutingService(
        k9SakClient = k9SakClientMock,
        infotrygdClient = infotrygdClientMock,
        overstyrTilK9SakJournalpostIds = setOf(
            journalpostOverstyrtTilK9Sak1,
            journalpostOverstyrtTilK9Sak2
        )
    )

    @BeforeEach
    internal fun reset() {
        clearMocks(k9SakClientMock)
        coEvery { k9SakClientMock.inngårIUnntaksliste(setOf(aktørIdIUnntaksliste), Søknadstype.PleiepengerLivetsSluttfase, any()) }.returns(true)
        coEvery { k9SakClientMock.inngårIUnntaksliste(setOf(aktørIdIkkeIUnntaksliste), Søknadstype.PleiepengerLivetsSluttfase, any()) }.returns(false)
        coEvery { k9SakClientMock.inngårIUnntaksliste(any(), not(Søknadstype.PleiepengerLivetsSluttfase), any()) }.returns(false)
        coEvery { k9SakClientMock.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any(), any()) }.returns(RutingGrunnlag(søker = true))
        clearMocks(infotrygdClientMock)
        coEvery { infotrygdClientMock.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any(), any()) }.returns(RutingGrunnlag(søker = true))
    }

    @Test
    fun `PILS søknad i unntaksliste og overstyrt til K9Sak skal rutes til Infotrygd`() {
        assertEquals(RutingService.Destinasjon.Infotrygd, hentDestinasjon(
            journalpostIds = setOf(journalpostOverstyrtTilK9Sak1, journalpostOverstyrtTilK9Sak2),
            søknadsType = Søknadstype.PleiepengerLivetsSluttfase,
            iUnntaksliste = true
        ))
    }

    @Test
    fun `Ikke i unntaksliste og overstyrt til K9Sak skal rutes til K9Sak`() {
        assertEquals(RutingService.Destinasjon.K9Sak, hentDestinasjon(
            journalpostIds = setOf(journalpostOverstyrtTilK9Sak1, journalpostOverstyrtTilK9Sak2),
            søknadsType = Søknadstype.PleiepengerSyktBarn
        ))
    }

    @Test
    fun `Feiler om et subset av journalpostene er overstyrt til K9Sak`() {
        assertThrows<IllegalStateException> {
            hentDestinasjon(
                journalpostIds = setOf(journalpostOverstyrtTilK9Sak1, journalpostIkkeOverstyrtTilK9Sak),
                søknadsType = Søknadstype.PleiepengerSyktBarn
            )
        }
    }

    @Test
    fun `Ikke i unntaksliste eller overstyrt til K9Sak gir normal ruting`() {
        coVerify { k9SakClientMock.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any(), any()) wasNot Called }
        assertEquals(RutingService.Destinasjon.K9Sak, hentDestinasjon(
            journalpostIds = setOf(journalpostIkkeOverstyrtTilK9Sak),
            søknadsType = Søknadstype.PleiepengerSyktBarn
        ))
        coVerify(exactly = 1) { k9SakClientMock.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any(), any()) }
        coVerify { infotrygdClientMock.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any(), any()) wasNot Called }
    }


    @Test
    fun `Tom liste med journalposter går rett på normal ruting`() {
        coVerify { k9SakClientMock.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any(), any()) wasNot Called }
        assertEquals(RutingService.Destinasjon.K9Sak, hentDestinasjon(
            journalpostIds = emptySet(),
            søknadsType = Søknadstype.PleiepengerSyktBarn
        ))
        coVerify(exactly = 1) { k9SakClientMock.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any(), any()) }
        assertEquals(RutingService.Destinasjon.K9Sak, hentDestinasjon(
            journalpostIds = emptySet(),
            søknadsType = Søknadstype.PleiepengerSyktBarn
        ))
        coVerify { infotrygdClientMock.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any(), any()) wasNot Called }
    }

    @Test
    fun `PILS søknad med sak i infotrygd rutes til Infotrygd, OMS & PSB til K9sak`() {
        coEvery { k9SakClientMock.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any(), any()) }.returns(RutingGrunnlag(søker = false, pleietrengende = false, annenPart = false))
        coEvery { infotrygdClientMock.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any(), any()) }.returns(RutingGrunnlag(søker = true))
        assertEquals(RutingService.Destinasjon.K9Sak, hentDestinasjon(
            journalpostIds = emptySet(),
            søknadsType = Søknadstype.PleiepengerSyktBarn
        ))
        assertEquals(RutingService.Destinasjon.Infotrygd, hentDestinasjon(
            journalpostIds = emptySet(),
            søknadsType = Søknadstype.PleiepengerLivetsSluttfase
        ))
        assertEquals(RutingService.Destinasjon.K9Sak, hentDestinasjon(
            journalpostIds = emptySet(),
            søknadsType = Søknadstype.Omsorgspenger
        ))
    }

    private fun hentDestinasjon(
        journalpostIds: Set<JournalpostId>,
        iUnntaksliste: Boolean = false,
        søknadsType: Søknadstype
    ) = runBlocking { rutingService.destinasjon(
        søker = "12345678911".somIdentitetsnummer(),
        fraOgMed = LocalDate.now(),
        søknadstype = søknadsType,
        aktørIder = when (iUnntaksliste) {
            true -> setOf(aktørIdIUnntaksliste)
            false -> setOf(aktørIdIkkeIUnntaksliste)
        },
        journalpostIds = journalpostIds,
        correlationId = "${UUID.randomUUID()}".somCorrelationId()
    )}
}
