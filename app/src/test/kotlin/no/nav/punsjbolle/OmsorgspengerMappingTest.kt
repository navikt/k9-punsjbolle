package no.nav.punsjbolle

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.Periode.Companion.somPeriode
import no.nav.punsjbolle.søknad.PunsjetSøknadMelding
import no.nav.punsjbolle.søknad.somPunsjetSøknad
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

internal class OmsorgspengerMappingTest {
    @Test
    fun `Utbetaling korrigering im`() {
        @Language("JSON")
        val json = """
        {
          "søknadId": "$søknadId",
          "mottattDato": "$mottatt",
          "søker": {
            "norskIdentitetsnummer": "$søker"
          },
          "ytelse": {
		"type": "OMP_UT",
		"fosterbarn": null,
		"aktivitet": null,
		"fraværsperioder": null,
		"fraværsperioderKorrigeringIm": [
			{
				"periode": "2021-11-08/2021-11-08",
				"duration": "PT0S",
				"årsak": null,
				"søknadÅrsak": null,
				"aktivitetFravær": [
					"ARBEIDSTAKER"
				],
				"arbeidsforholdId": "",
				"arbeidsgiverOrgNr": "979312059"
			}
			],
		"bosteder": null,
		"utenlandsopphold": null
	},
          "journalposter": [{"journalpostId": "$journalpostId"}] 
        }
        """.trimIndent()


        val jacksonSøknad = json.jackson()

        val forventetPunsjetSøknad = PunsjetSøknadMelding.PunsjetSøknad(
            versjon = "1.0.0",
            søknadId = søknadId,
            saksnummer = null,
            søknadstype = Søknadstype.OmsorgspengerUtbetaling_Korrigering,
            journalpostIder = setOf(journalpostId),
            periode = "2021-11-08/2021-11-08".somPeriode(),
            søker = søker,
            annenPart = null,
            pleietrengende = null,
            søknadJson = jacksonSøknad,
            mottatt = ZonedDateTime.parse(mottatt),
            saksbehandler = "n/a"
        )

        assertEquals(forventetPunsjetSøknad, jacksonSøknad.somPunsjetSøknad(
            "1.0.0",
            saksbehandler = "n/a",
            saksnummer = null,
            null
        ))
    }

    @Test
    fun `Kronisk sykt barn`() {
        @Language("JSON")
        val json = """
        {
          "søknadId": "$søknadId",
          "mottattDato": "$mottatt",
          "søker": {
            "norskIdentitetsnummer": "$søker"
          },
          "ytelse": {
            "type": "OMP_UTV_KS",
            "barn": {
              "norskIdentitetsnummer": "$barn"
            }
          },
          "journalposter": [{"journalpostId": "$journalpostId"}] 
        }
        """.trimIndent()


        val jacksonSøknad = json.jackson()

        val forventetPunsjetSøknad = PunsjetSøknadMelding.PunsjetSøknad(
            versjon = "1.0.0",
            søknadId = søknadId,
            saksnummer = null,
            søknadstype = Søknadstype.OmsorgspengerKroniskSyktBarn,
            journalpostIder = setOf(journalpostId),
            periode = "2021-05-03/..".somPeriode(),
            søker = søker,
            annenPart = null,
            pleietrengende = barn,
            søknadJson = jacksonSøknad,
            mottatt = ZonedDateTime.parse(mottatt),
            saksbehandler = "Saks Behandlersen"
        )

        assertEquals(forventetPunsjetSøknad, jacksonSøknad.somPunsjetSøknad(
            "1.0.0",
            saksbehandler = "Saks Behandlersen",
            saksnummer = null,
            null
        ))
    }

    @Test
    fun `Midlertidig alene`() {
        @Language("JSON")
        val json = """
        {
          "søknadId": "$søknadId",
          "mottattDato": "$mottatt",
          "søker": {
            "norskIdentitetsnummer": "$søker"
          },
          "ytelse": {
            "type": "OMP_UTV_MA",
            "annenForelder": {
              "norskIdentitetsnummer": "$annenForelder",
              "periode": "2020-01-01/2050-04-04"
            }
          },
          "journalposter": [{"journalpostId": "$journalpostId"}] 
        }
        """.trimIndent()


        val jacksonSøknad = json.jackson()

        val forventetPunsjetSøknad = PunsjetSøknadMelding.PunsjetSøknad(
            versjon = "1.0.0",
            søknadId = søknadId,
            saksnummer = null,
            søknadstype = Søknadstype.OmsorgspengerMidlertidigAlene,
            journalpostIder = setOf(journalpostId),
            periode = "2020-01-01/2050-04-04".somPeriode(),
            søker = søker,
            annenPart = annenForelder,
            pleietrengende = null,
            søknadJson = jacksonSøknad,
            mottatt = ZonedDateTime.parse(mottatt),
            saksbehandler = "n/a"
        )

        assertEquals(forventetPunsjetSøknad, jacksonSøknad.somPunsjetSøknad(
            "1.0.0",
            saksbehandler = "n/a",
            saksnummer = null,
            null
        ))
    }

    private companion object {
        private val mottatt = "2021-05-03T16:08:45.800Z"
        private val søknadId = "2ce43013-951c-4569-8afe-de701554e071"
        private val søker = "11111111111".somIdentitetsnummer()
        private val journalpostId = "22222222222".somJournalpostId()
        private val barn = "33333333333".somIdentitetsnummer()
        private val annenForelder = "44444444444".somIdentitetsnummer()
        private fun String.jackson() = jacksonObjectMapper().readTree(this) as ObjectNode
    }
}
