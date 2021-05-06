package no.nav.punsjbolle

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.søknad.PunsjetSøknadMelding
import no.nav.punsjbolle.søknad.somPunsjetSøknad
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import java.util.*
import kotlin.test.assertEquals

internal class PunsjetPleiepengerSyktBarnMappingTest {

    @Test
    fun `Ny søknad`() {
        val søknadId = "1e7652a9-d834-42a1-997b-b29b93c58b33"
        val journalpostIder = setOf(journalpostId)
        val søknadsperioder = setOf(Periode("2021-01-01/2050-12-15"), Periode("2022-04-04/.."))

        val søknadJson = pleiepengerSyktBarnSøknad(
                søknadId = søknadId,
                journalpostIder = journalpostIder,
                søker = søker,
                barn = barn,
                søknadsperioder = søknadsperioder,
                endringsperioder = null
        )

        @Language("JSON")
        val forventetJson = """
          {"ytelse":{"type":"PLEIEPENGER_SYKT_BARN","barn":{"norskIdentitetsnumemr":"33333333333"},"søknadsperiode":["2021-01-01/2050-12-15","2022-04-04/.."]},"journalposter":[{"journalpostId":"22222222222"}],"søknadId":"1e7652a9-d834-42a1-997b-b29b93c58b33","søker":{"norskIdentitetsnummer":"11111111111"}}
        """.trimIndent()

        JSONAssert.assertEquals(forventetJson, søknadJson.toString(), true)

        val jacksonSøknad = søknadJson.jackson()

        val forventetPunsjetSøknad = PunsjetSøknadMelding.PunsjetSøknad(
                versjon = "1.0.0",
                søknadId = søknadId,
                saksnummer = null,
                søknadstype = Søknadstype.PleiepengerSyktBarn,
                journalpostIder = journalpostIder,
                periode = Periode("2021-01-01/.."),
                søker = søker,
                annenPart = null,
                pleietrengende = null,
                søknadJson = jacksonSøknad
        )

        assertEquals(forventetPunsjetSøknad, jacksonSøknad.somPunsjetSøknad("1.0.0", saksnummer = null))
    }

    @Test
    fun `Endringssøknad uten barn`() {
        val søknadId = "1e7652a9-d834-42a1-997b-b29b93c58b32"
        val journalpostIder = setOf(journalpostId)
        val endringsperioder = setOf(Periode("2021-01-01/.."))

        val søknadJson = pleiepengerSyktBarnSøknad(
                søknadId = søknadId,
                journalpostIder = journalpostIder,
                søker = søker,
                barn = null,
                søknadsperioder = null,
                endringsperioder = endringsperioder
        )

        @Language("JSON")
        val forventetJson = """
          {"ytelse":{"type":"PLEIEPENGER_SYKT_BARN","endringsperiode":["2021-01-01/.."]},"journalposter":[{"journalpostId":"22222222222"}],"søknadId":"1e7652a9-d834-42a1-997b-b29b93c58b32","søker":{"norskIdentitetsnummer":"11111111111"}}
        """.trimIndent()

        JSONAssert.assertEquals(forventetJson, søknadJson.toString(), true)

        val jacksonSøknad = søknadJson.jackson()

        val forventetPunsjetSøknad = PunsjetSøknadMelding.PunsjetSøknad(
            versjon = "1.0.0",
            søknadId = søknadId,
            saksnummer = null,
            søknadstype = Søknadstype.PleiepengerSyktBarn,
            journalpostIder = journalpostIder,
            periode = endringsperioder.first(),
            søker = søker,
            annenPart = null,
            pleietrengende = null,
            søknadJson = jacksonSøknad
        )

        assertEquals(forventetPunsjetSøknad, jacksonSøknad.somPunsjetSøknad("1.0.0", saksnummer = null))


    }

    @Test
    fun `Mangler periode`() {
        val søknadId = "1e7652a9-d834-42a1-997b-b29b93c58b34"
        val journalpostIder = setOf(journalpostId)

        val søknadJson = pleiepengerSyktBarnSøknad(
                søknadId = søknadId,
                journalpostIder = journalpostIder,
                søker = søker,
                barn = barn,
                søknadsperioder = null,
                endringsperioder = null
        )

        @Language("JSON")
        val forventetJson = """
          {"ytelse":{"type":"PLEIEPENGER_SYKT_BARN","barn":{"norskIdentitetsnumemr":"33333333333"}},"journalposter":[{"journalpostId":"22222222222"}],"søknadId":"1e7652a9-d834-42a1-997b-b29b93c58b34","søker":{"norskIdentitetsnummer":"11111111111"}}
        """.trimIndent()

        JSONAssert.assertEquals(forventetJson, søknadJson.toString(), true)

        assertThrows<IllegalArgumentException> {
            søknadJson.jackson().somPunsjetSøknad("1.0.0", null)
        }
    }

    internal companion object {
        private val søker = "11111111111".somIdentitetsnummer()
        private val journalpostId = "22222222222".somJournalpostId()
        private val barn = "33333333333".somIdentitetsnummer()

        internal fun pleiepengerSyktBarnSøknad(
            søknadId: String = "${UUID.randomUUID()}",
            journalpostIder: Set<JournalpostId>,
            søker: Identitetsnummer,
            barn: Identitetsnummer?,
            søknadsperioder: Set<Periode>?,
            endringsperioder: Set<Periode>?) : JSONObject {

            val ytelse = JSONObject(mapOf(
                "type" to "PLEIEPENGER_SYKT_BARN"
            ))
            søknadsperioder?.also {
                ytelse.put("søknadsperiode", it.map { periode -> "$periode" })
            }
            endringsperioder?.also {
                ytelse.put("endringsperiode", it.map { periode -> "$periode" })
            }
            barn?.also {
                ytelse.put("barn", mapOf(
                    "norskIdentitetsnumemr" to "$barn"
                ))
            }

            val søknad = JSONObject(mapOf(
                "søknadId" to søknadId,
                "søker" to mapOf(
                    "norskIdentitetsnummer" to "$søker"
                ),
                "journalposter" to journalpostIder.map { mapOf(
                    "journalpostId" to "$it"
                )},
                "ytelse" to ytelse.toMap()
            ))

            return søknad
        }

        private fun JSONObject.jackson() = jacksonObjectMapper().readTree(toString()) as ObjectNode
    }
}
