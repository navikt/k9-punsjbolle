package no.nav.punsjbolle

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.Periode.Companion.somPeriode
import no.nav.punsjbolle.PunsjetPleiepengerSyktBarnMappingTest.Companion.pleiepengerSyktBarnSøknad
import no.nav.punsjbolle.testutils.ApplicationContextExtension
import no.nav.punsjbolle.testutils.rapid.*
import no.nav.punsjbolle.testutils.rapid.mockFerdigstillJournalføringForK9OgJournalførJson
import no.nav.punsjbolle.testutils.rapid.mockHentAktørIder
import no.nav.punsjbolle.testutils.rapid.printSisteMelding
import no.nav.punsjbolle.testutils.rapid.sisteMeldingHarLøsningPå
import no.nav.punsjbolle.testutils.søknad.PunsjetSøknadVerktøy.punsjetSøknad
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class PunsjetPleiepengerSyktBarnTest(
    private val builder: ApplicationContext.Builder) {

    private val rapid = TestRapid().also {
        it.registerApplicationContext(builder.build())
    }

    @Test
    fun `Håndtere en søknad for pleiepenger sykt barn`() {
        val søker = "111111111111".somIdentitetsnummer()
        val barn = "222222222222".somIdentitetsnummer()

        rapid.sendTestMessage(punsjetSøknad(pleiepengerSyktBarnSøknad(
                barn = barn,
                søker = søker,
                journalpostIder = setOf("1112131415".somJournalpostId()),
                søknadsperioder = setOf("2018-12-30/2019-10-20".somPeriode()),
                endringsperioder = null
        )))

        rapid.mockHentAktørIder(setOf(søker, barn))
        rapid.mockFerdigstillJournalføringForK9OgJournalførJson()

        rapid.printSisteMelding()

        rapid.sisteMeldingHarLøsningPå("PunsjetSøknad")
        rapid.sisteMeldingErKlarForArkivering()
    }
}