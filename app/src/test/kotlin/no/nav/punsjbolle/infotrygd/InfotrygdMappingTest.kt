package no.nav.punsjbolle.infotrygd

import no.nav.punsjbolle.Søknadstype
import no.nav.punsjbolle.infotrygd.InfotrygdClient.Companion.inneholderAktuelleSakerEllerVedtak
import no.nav.punsjbolle.infotrygd.InfotrygdClient.Companion.inneholderAktuelleVedtak
import org.intellij.lang.annotations.Language
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

internal class InfotrygdMappingTest {

    @Test
    fun `Vedtak og saker på søker`() {
        assertFalse(JSONArray(SakerEksempelResponse).inneholderAktuelleSakerEllerVedtak(Søknadstype.PleiepengerSyktBarn))
    }

    @Test
    fun `Vedtak på barn`() {
        assertTrue(JSONArray(VedtakBarnEksempelResponse).inneholderAktuelleVedtak(Søknadstype.PleiepengerSyktBarn))
        assertFalse(JSONArray(VedtakBarnEksempelResponse).inneholderAktuelleVedtak(Søknadstype.OmsorgspengerKroniskSyktBarn))

    }

    @Test
    fun `Vedtak på barn under annet tema og behandlignstema`() {
        assertFalse(vedtakBarnMinimalResponse(behandlingstema = null, tema = null).inneholderAktuelleVedtak(Søknadstype.PleiepengerSyktBarn))
        assertFalse(vedtakBarnMinimalResponse(behandlingstema = "Feil", tema = "BS").inneholderAktuelleVedtak(Søknadstype.PleiepengerSyktBarn))
    }

    @Test
    fun `Vedtak og saker på søker under annet tema og behandlingstema`() {
        assertFalse(sakerMinimalResponse(behandlingstemaSak = "PP", behandlingstemaVedtak = "PP", temaSak = null, temaVedtak = null).inneholderAktuelleSakerEllerVedtak(Søknadstype.PleiepengerSyktBarn))
        assertFalse(sakerMinimalResponse(behandlingstemaSak = "PP", behandlingstemaVedtak = "Feil", temaSak = "BS2", temaVedtak = "BS2").inneholderAktuelleSakerEllerVedtak(Søknadstype.PleiepengerSyktBarn))
    }

    private companion object {
        @Language("JSON")
        private val SakerEksempelResponse = """
            [{
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
            }]
        """.trimIndent()

        @Language("JSON")
        private fun sakerMinimalResponse(
            behandlingstemaSak: String?, temaSak: String?,
            behandlingstemaVedtak: String?, temaVedtak: String?) = JSONArray(
            """
                [{
                  "saker": [{
                    "behandlingstema": {"kode": ${behandlingstemaSak?.let { """"$it"""" }}},
                    "tema": {"kode": ${temaSak?.let { """"$it"""" }} }
                  }],
                  "vedtak": [{
                    "behandlingstema": {"kode": ${behandlingstemaVedtak?.let { """"$it"""" }}},
                    "tema": {"kode": ${temaVedtak?.let { """"$it"""" }} }
                  }]
                }]
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
                      "kode": "PB",
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
