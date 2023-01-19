package no.nav.punsjbolle

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.Periode.Companion.somPeriode
import no.nav.punsjbolle.testutils.ApplicationContextExtension
import no.nav.punsjbolle.testutils.rapid.*
import no.nav.punsjbolle.testutils.rapid.mockFerdigstillJournalføringForK9OgJournalførJson
import no.nav.punsjbolle.testutils.rapid.mockHentAktørIder
import no.nav.punsjbolle.testutils.rapid.printSisteMelding
import no.nav.punsjbolle.testutils.rapid.sisteMeldingHarLøsningPå
import no.nav.punsjbolle.testutils.søknad.PunsjetSøknadVerktøy.punsjetSøknad
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(ApplicationContextExtension::class)
internal class PunsjetOpplaeringspengerTest(
    private val builder: ApplicationContext.Builder
) {

    private val rapid = TestRapid().also {
        it.registerApplicationContext(builder.build())
    }

    @Test
    fun `Håndtere en søknad for opplæringspenger`() {
        val søker = "111111111111".somIdentitetsnummer()
        val barn = "222222222222".somIdentitetsnummer()

        rapid.sendTestMessage(
            punsjetSøknad(
                søknad = opplæringspengerSøknad(
                    barn = barn,
                    søker = søker,
                    journalpostIder = setOf("1112131415".somJournalpostId()),
                    søknadsperioder = setOf("2018-12-30/2019-10-20".somPeriode()),
                ),
                brevkode = Brevkode.OPPLÆRINGSPENGER_SOKNAD
            )
        )

        rapid.mockHentAktørIder(setOf(søker, barn))
        rapid.mockFerdigstillJournalføringForK9OgJournalførJson()

        rapid.printSisteMelding()

        rapid.sisteMeldingHarLøsningPå("PunsjetSøknad")
        rapid.sisteMeldingErKlarForArkivering()
    }

    internal companion object {
        internal fun opplæringspengerSøknad(
            søknadId: String = "${UUID.randomUUID()}",
            journalpostIder: Set<JournalpostId>,
            søker: Identitetsnummer,
            barn: Identitetsnummer? = null,
            søknadsperioder: Set<Periode>? = null,
        ): JSONObject {

            val ytelse = JSONObject(
                mapOf(
                    "type" to "OPPLÆRINGSPENGER"
                )
            )
            søknadsperioder?.also {
                ytelse.put("søknadsperiode", it.map { periode -> "$periode" })
            }
            barn?.also {
                ytelse.put(
                    "barn", mapOf(
                        "norskIdentitetsnummer" to "$barn"
                    )
                )
            }

            val søknad = JSONObject(
                mapOf(
                    "søknadId" to søknadId,
                    "mottattDato" to "2021-05-03T16:08:45.800Z",
                    "søker" to mapOf(
                        "norskIdentitetsnummer" to "$søker"
                    ),
                    "journalposter" to journalpostIder.map {
                        mapOf(
                            "journalpostId" to "$it"
                        )
                    },
                    "ytelse" to ytelse.toMap()
                )
            )

            return søknad
        }

        private fun JSONObject.jackson() = jacksonObjectMapper().readTree(toString()) as ObjectNode
    }
}