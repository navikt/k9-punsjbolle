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
    fun `Ikke i unntaksliste og overstyrt til K9Sak skal rutes til K9Sak`() {
        assertEquals(RutingService.Destinasjon.K9Sak, hentDestinasjon(
            journalpostIds = setOf(journalpostOverstyrtTilK9Sak1, journalpostOverstyrtTilK9Sak2),
            søknadsType = Søknadstype.PleiepengerSyktBarn
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
