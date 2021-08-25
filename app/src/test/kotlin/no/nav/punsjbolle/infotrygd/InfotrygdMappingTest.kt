package no.nav.punsjbolle.infotrygd

import no.nav.punsjbolle.infotrygd.InfotrygdClient.Companion.inneholderAktuelleSakerEllerVedtak
import no.nav.punsjbolle.infotrygd.InfotrygdClient.Companion.inneholderAktuelleVedtak
import org.intellij.lang.annotations.Language
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class InfotrygdMappingTest {

    @Test
    fun `Vedtak og saker på søker`() {
        assertTrue(JSONObject(SakerEksempelResponse).inneholderAktuelleSakerEllerVedtak())
    }

    @Test
    fun `Vedtak på barn`() {
        assertTrue(JSONArray(VedtakBarnEksempelResponse).inneholderAktuelleVedtak())
    }

    @Test
    fun `Vedtak på barn under annet tema og behandlignstema`() {
        assertFalse(vedtakBarnMinimalResponse(behandlingstema = null, tema = null).inneholderAktuelleVedtak())
        assertFalse(vedtakBarnMinimalResponse(behandlingstema = "PN", tema = null).inneholderAktuelleVedtak())
        assertFalse(vedtakBarnMinimalResponse(behandlingstema = "Feil", tema = "BS").inneholderAktuelleVedtak())
        assertTrue(vedtakBarnMinimalResponse(behandlingstema = "PN", tema = "BS").inneholderAktuelleVedtak())
    }

    @Test
    fun `Vedtak og saker på søker under annet tema og behandlingstema`() {
        assertFalse(sakerMinimalResponse(behandlingstemaSak = "PP", behandlingstemaVedtak = "PP", temaSak = null, temaVedtak = null).inneholderAktuelleSakerEllerVedtak())
        assertTrue(sakerMinimalResponse(behandlingstemaSak = "PN", behandlingstemaVedtak = "Feil", temaSak = "BS", temaVedtak = "BS").inneholderAktuelleSakerEllerVedtak())
        assertFalse(sakerMinimalResponse(behandlingstemaSak = "PP", behandlingstemaVedtak = "Feil", temaSak = "BS2", temaVedtak = "BS2").inneholderAktuelleSakerEllerVedtak())
    }

    @Test
    fun `Vedtak og saker som er henlagt eller bortfalt uten opphørsdato bør filtreres bort`() {
        @Language("JSON")
        val henlagtEllerBortfaltUtenOpphørsdato = """
            {
              "saker": [{
                "behandlingstema": {"kode": "PN"},
                "tema": {"kode": "BS" },
                "resultat": {"kode": "HB"}
              }],
              "vedtak": [{
                "behandlingstema": {"kode": "PN"},
                "tema": {"kode": "BS" },
                "resultat": {"kode": "HB"}
              }]
            }
        """.trimIndent()

        @Language("JSON")
        val henlagtEllerBortfaltMedOpphørsdato = """
            {
              "saker": [{
                "behandlingstema": {"kode": "PN"},
                "tema": {"kode": "BS" },
                "resultat": {"kode": "HB"},
                "opphoerFom": "2020-02-02"
              }],
              "vedtak": [{
                "behandlingstema": {"kode": "PN"},
                "tema": {"kode": "BS" },
                "resultat": {"kode": "HB"},
                "opphoerFom": "2020-02-02"
              }]
            }
        """.trimIndent()

        @Language("JSON")
        val ikkeHenlagtEllerBortfalt = """
            {
              "saker": [{
                "behandlingstema": {"kode": "PN"},
                "tema": {"kode": "BS" },
                "resultat": {"kode": "NOE"},
                "opphoerFom": null
              }],
              "vedtak": [{
                "behandlingstema": {"kode": "PN"},
                "tema": {"kode": "BS" },
                "resultat": {"kode": "ANNET"},
                "opphoerFom": null
              }]
            }
        """.trimIndent()

        assertFalse(JSONObject(henlagtEllerBortfaltUtenOpphørsdato).inneholderAktuelleSakerEllerVedtak())
        assertTrue(JSONObject(henlagtEllerBortfaltMedOpphørsdato).inneholderAktuelleSakerEllerVedtak())
        assertTrue(JSONObject(ikkeHenlagtEllerBortfalt).inneholderAktuelleSakerEllerVedtak())
    }

    private companion object {
        @Language("JSON")
        private val SakerEksempelResponse = """
            {
              "saker": [
                {
                  "behandlingstema": {
                    "kode": "PN",
                    "termnavn": "string"
                  },
                  "iverksatt": "2020-01-01",
                  "opphoerFom": "2020-01-01",
                  "registrert": "2020-01-01",
                  "resultat": {
                    "kode": "X",
                    "termnavn": "string"
                  },
                  "sakId": "X11",
                  "status": {
                    "kode": "status",
                    "termnavn": "string"
                  },
                  "tema": {
                    "kode": "BS",
                    "termnavn": "string"
                  },
                  "type": {
                    "kode": "X",
                    "termnavn": "string"
                  },
                  "vedtatt": "2020-01-01"
                }
              ],
              "vedtak": [
                {
                  "behandlingstema": {
                    "kode": "PN",
                    "termnavn": "string"
                  },
                  "iverksatt": "2020-01-01",
                  "opphoerFom": "2020-01-01",
                  "registrert": "2020-01-01",
                  "resultat": {
                    "kode": "X",
                    "termnavn": "string"
                  },
                  "sakId": "X11",
                  "status": {
                    "kode": "status",
                    "termnavn": "string"
                  },
                  "tema": {
                    "kode": "BS",
                    "termnavn": "string"
                  },
                  "type": {
                    "kode": "X",
                    "termnavn": "string"
                  },
                  "vedtatt": "2020-01-01"
                }
              ]
            }
        """.trimIndent()

        @Language("JSON")
        private fun sakerMinimalResponse(
            behandlingstemaSak: String?, temaSak: String?,
            behandlingstemaVedtak: String?, temaVedtak: String?) = JSONObject(
            """
                {
                  "saker": [{
                    "behandlingstema": {"kode": ${behandlingstemaSak?.let { """"$it"""" }}},
                    "tema": {"kode": ${temaSak?.let { """"$it"""" }} }
                  }],
                  "vedtak": [{
                    "behandlingstema": {"kode": ${behandlingstemaVedtak?.let { """"$it"""" }}},
                    "tema": {"kode": ${temaVedtak?.let { """"$it"""" }} }
                  }]
                }
            """.trimIndent()
        )

        @Language("JSON")
        private val VedtakBarnEksempelResponse = """
            [
              {
                "soekerFnr": "12345678900",
                "vedtak": [
                  {
                    "behandlingstema": {
                      "kode": "PN",
                      "termnavn": "string"
                    },
                    "iverksatt": "2020-01-01",
                    "opphoerFom": "2020-01-01",
                    "registrert": "2020-01-01",
                    "resultat": {
                      "kode": "X",
                      "termnavn": "string"
                    },
                    "sakId": "X11",
                    "status": {
                      "kode": "status",
                      "termnavn": "string"
                    },
                    "tema": {
                      "kode": "BS",
                      "termnavn": "string"
                    },
                    "type": {
                      "kode": "X",
                      "termnavn": "string"
                    },
                    "vedtatt": "2020-01-01"
                  }
                ]
              }
            ]
        """.trimIndent()

        @Language("JSON")
        private fun vedtakBarnMinimalResponse(behandlingstema: String?, tema: String?) = JSONArray(
            """
            [{
                "vedtak": [{
                  "behandlingstema": {"kode": ${behandlingstema?.let { """"$it"""" }}},
                  "tema": {"kode": ${tema?.let { """"$it"""" }} }
                }]   
            }]
        """.trimIndent()
        )
    }
}