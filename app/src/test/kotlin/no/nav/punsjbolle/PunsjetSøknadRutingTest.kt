package no.nav.punsjbolle

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.punsjbolle.Periode.Companion.somPeriode
import no.nav.punsjbolle.infotrygd.InfotrygdClient
import no.nav.punsjbolle.k9sak.K9SakClient
import no.nav.punsjbolle.ruting.RutingGrunnlag
import no.nav.punsjbolle.testutils.*
import no.nav.punsjbolle.testutils.ApplicationContextExtension
import no.nav.punsjbolle.testutils.printSisteMelding
import no.nav.punsjbolle.testutils.rapid.mockFerdigstillJournalføringForK9
import no.nav.punsjbolle.testutils.rapid.mockHentAktørIder
import no.nav.punsjbolle.testutils.sisteMeldingHarLøsningPå
import no.nav.punsjbolle.testutils.sisteMeldingManglerLøsningPå
import no.nav.punsjbolle.testutils.søknad.PunsjetSøknadVerktøy
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert

@ExtendWith(ApplicationContextExtension::class)
internal class PunsjetSøknadRutingTest(
    private val builder: ApplicationContext.Builder) {

    private val infotrygdClientMock : InfotrygdClient = mockk()
    private val k9SakClientMock : K9SakClient = mockk()

    private val rapid = TestRapid().also {
        it.registerApplicationContext(builder.also { b ->
            b.k9SakClient = k9SakClientMock
            b.infotrygdClient = infotrygdClientMock
        }.build())
    }

    @BeforeEach
    fun beforeEach() {
        clearMocks(infotrygdClientMock, k9SakClientMock)
        rapid.reset()
    }

    @Test
    fun `Barnet har en løpende sak i Infotrygd`() {
        mock(
            k9sak = RutingGrunnlag(søker = false, pleietrengende = false, annenPart = false),
            infotrygd = RutingGrunnlag(søker = false, pleietrengende = true, annenPart = false)
        )

        rapid.sendPunsjetSøknad()
        rapid.mockHentAktørIder(setOf(søker, barn))
        rapid.assertGosysJournalføringsoppgave()
        rapid.printSisteMelding()
    }

    @Test
    fun `Søker har en løpende sak i Infotrygd`() {
        mock(
            k9sak = RutingGrunnlag(søker = false, pleietrengende = false, annenPart = false),
            infotrygd = RutingGrunnlag(søker = true, pleietrengende = false, annenPart = false)
        )

        rapid.sendPunsjetSøknad()
        rapid.mockHentAktørIder(setOf(søker, barn))
        rapid.assertGosysJournalføringsoppgave()
        rapid.printSisteMelding()
    }

    @Test
    fun `Barn har en løpende sak i Infotrygd, søker i K9Sak`() {
        mock(
            k9sak = RutingGrunnlag(søker = true, pleietrengende = false, annenPart = false),
            infotrygd = RutingGrunnlag(søker = false, pleietrengende = true, annenPart = false)
        )
        coEvery { k9SakClientMock.hentSaksnummer(any(),any()) }.returns("123SAK".somK9Saksnummer())
        coEvery { k9SakClientMock.sendInnSøknad(any(), any(), any()) }.returns(Unit)

        rapid.sendPunsjetSøknad()
        rapid.mockHentAktørIder(setOf(søker, barn))
        rapid.mockFerdigstillJournalføringForK9()
        rapid.sisteMeldingHarLøsningPå("PunsjetSøknad")
    }

    @Test
    fun `Ingen har sak hverken i K9Sak eller Infotrygd, men inngårt i unntaksliste`() {
        mock(
            k9sak = RutingGrunnlag(søker = false, pleietrengende = false, annenPart = false),
            infotrygd = RutingGrunnlag(søker = false, pleietrengende = false, annenPart = false),
            inngårIUnntaksliste = true
        )
        rapid.sendPunsjetSøknad()
        rapid.mockHentAktørIder(setOf(søker, barn))
        rapid.assertGosysJournalføringsoppgave()
        rapid.printSisteMelding()
    }

    private fun mock(infotrygd: RutingGrunnlag, k9sak: RutingGrunnlag, inngårIUnntaksliste: Boolean = false) {
        coEvery { infotrygdClientMock.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any()) }.returns(infotrygd)
        coEvery { k9SakClientMock.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any(), any()) }.returns(k9sak)
        coEvery { k9SakClientMock.inngårIUnntaksliste(any(), any(), any()) }.returns(inngårIUnntaksliste)
    }

    private companion object {
        val søker = "111111111111".somIdentitetsnummer()
        val barn = "222222222222".somIdentitetsnummer()
        val journalpostId ="1112131415".somJournalpostId()

        @Language("JSON")
        val forventetOpprettGosysOppgaverInput = """
            {
               "identitetsnummer": "111111111111",
               "berørteIdentitetsnummer": ["222222222222"],
               "journalpostType": "PleiepengerSyktBarn",
               "journalpostIder": ["1112131415"]
            }
        """.trimIndent()

        @Language("JSON")
        val forventetBehovsrekkefølge = """
            [
              "HentPersonopplysninger",
              "PunsjetSøknad",
              "OpprettGosysJournalføringsoppgaver"
            ]
        """.trimIndent()

        private fun TestRapid.sendPunsjetSøknad() {
            sendTestMessage(
                PunsjetSøknadVerktøy.punsjetSøknad(
                    PunsjetPleiepengerSyktBarnMappingTest.pleiepengerSyktBarnSøknad(
                        barn = barn,
                        søker = søker,
                        journalpostIder = setOf(journalpostId),
                        søknadsperioder = setOf("2018-12-30/2019-10-20".somPeriode()),
                        endringsperioder = null
                    )
                )
            )
        }

        private fun TestRapid.assertGosysJournalføringsoppgave() {
            sisteMeldingHarLøsningPå("PunsjetSøknad")
            sisteMeldingManglerLøsningPå("OpprettGosysJournalføringsoppgaver")
            val faktiskOpprettGosysOppgaverInput = sisteMeldingSomJSONObject()
                .getJSONObject("@behov")
                .getJSONObject("OpprettGosysJournalføringsoppgaver")
                .toString()

            val faktiskBehovsrekkefølge = sisteMeldingSomJSONObject()
                .getJSONArray("@behovsrekkefølge")
                .toString()

            JSONAssert.assertEquals(forventetOpprettGosysOppgaverInput, faktiskOpprettGosysOppgaverInput, true)
            JSONAssert.assertEquals(forventetBehovsrekkefølge, faktiskBehovsrekkefølge, true)
        }
    }
}