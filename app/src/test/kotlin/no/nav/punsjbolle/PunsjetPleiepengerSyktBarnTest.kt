package no.nav.punsjbolle

import de.huxhorn.sulky.ulid.ULID
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.testutils.ApplicationContextExtension
import no.nav.punsjbolle.testutils.printSisteMelding
import no.nav.punsjbolle.testutils.rapid.mockFerdigstillJournalføringForK9
import no.nav.punsjbolle.testutils.rapid.mockHentAktørIder
import no.nav.punsjbolle.testutils.sisteMeldingHarLøsningPå
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

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

        val pleiepengesøknad = beh(søker = søker, barn = barn)

        rapid.sendTestMessage(pleiepengesøknad)

        rapid.mockHentAktørIder(setOf(søker, barn))
        rapid.mockFerdigstillJournalføringForK9()

        rapid.sisteMeldingHarLøsningPå("PunsjetSøknad")

        rapid.printSisteMelding()
    }

    private fun beh(søker: Identitetsnummer, barn: Identitetsnummer) = Behovssekvens(
        id = ULID().nextULID(),
        correlationId = "${UUID.randomUUID()}",
        behov = arrayOf(Behov(
            navn = "PunsjetSøknad",
            input = mapOf(
                "versjon" to "1.0.0",
                "søknad" to mapOf(
                    "søknadId" to "${UUID.randomUUID()}",
                    "søker" to mapOf(
                        "norskIdentitetsnummer" to "$søker"
                    ),
                    "journalposter" to listOf(mapOf(
                        "journalpostId" to "1112131415"
                    )),
                    "ytelse" to mapOf(
                        "type" to "PLEIEPENGER_SYKT_BARN",
                        "barn" to mapOf(
                            "norskIdentitetsnummer" to "$barn"
                        ),
                        "søknadsperiode" to listOf(
                            "2018-12-30/2019-10-20"
                        )
                    )
                )
            )
        ))
    ).keyValue.second
}