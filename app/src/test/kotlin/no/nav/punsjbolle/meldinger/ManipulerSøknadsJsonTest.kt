package no.nav.punsjbolle.meldinger

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.punsjbolle.Søknadstype
import no.nav.punsjbolle.meldinger.JournalførJsonMelding.manipulerSøknadsJson
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

internal class ManipulerSøknadsJsonTest {

    @Test
    fun `manupulere søknad for pleiepenger sykt barn`() {
        val søknad = jacksonObjectMapper().readTree(før) as ObjectNode
        val manipulert = søknad.manipulerSøknadsJson(søknadstype = Søknadstype.PleiepengerSyktBarn)
        JSONAssert.assertEquals(forventetEtter, "$manipulert", true)
    }

    @Test
    fun `søknad uten manipulerte felter forblir uendret`() {
        @Language("JSON")
        val json = """
        {
            "foo": "bar",
            "bar": {
                "MottattDato": "2020-01-01",
                "virksomhetsnavn": "Virksomhet"
            }
        }
        """.trimIndent()
        val søknad = jacksonObjectMapper().readTree(json) as ObjectNode
        val manipulert = søknad.manipulerSøknadsJson(søknadstype = Søknadstype.PleiepengerSyktBarn)
        JSONAssert.assertEquals(json, "$manipulert", true)
    }


    private companion object {
        @Language("JSON")
        private val før = """
        {
          "søknadId": "1",
          "versjon": "2.0.0",
          "mottattDato": "2020-10-12T12:53:21.046Z",
          "søker": {
            "norskIdentitetsnummer": "11111111111"
          },
          "journalposter": [
            {
              "inneholderInfomasjonSomIkkeKanPunsjes": false,
              "inneholderMedisinskeOpplysninger": false,
              "journalpostId": "sajhdasd83724234"
            }
          ],
          "ytelse": {
            "type": "PLEIEPENGER_SYKT_BARN",
            "søknadsperiode": [
              "2018-12-30/2019-10-20"
            ],
            "endringsperiode": [
              
            ],
            "dataBruktTilUtledning": {
              "harForståttRettigheterOgPlikter": true,
              "harBekreftetOpplysninger": true,
              "samtidigHjemme": false,
              "harMedsøker": false,
              "bekrefterPeriodeOver8Uker": true
            },
            "barn": {
              "norskIdentitetsnummer": "22111111111",
              "fødselsdato": null
            },
            "arbeidAktivitet": {
              "selvstendigNæringsdrivende": [
                {
                  "perioder": {
                    "2018-11-11/2018-11-30": {
                      "virksomhetstyper": [
                        "FISKE"
                      ]
                    }
                  },
                  "virksomhetNavn": "Test"
                }
              ],
              "frilanser": {
                "startdato": "2019-10-10",
                "jobberFortsattSomFrilans": true
              }
            },
            "beredskap": {
              "perioder": {
                "2019-02-21/2019-05-21": {
                  "tilleggsinformasjon": "Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ."
                },
                "2018-12-30/2019-02-20": {
                  "tilleggsinformasjon": "Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ."
                }
              }
            },
            "nattevåk": {
              "perioder": {
                "2019-02-21/2019-05-21": {
                  "tilleggsinformasjon": "Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ."
                },
                "2018-12-30/2019-02-20": {
                  "tilleggsinformasjon": "Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ."
                }
              }
            },
            "tilsynsordning": {
              "perioder": {
                "2019-01-01/2019-01-01": {
                  "etablertTilsynTimerPerDag": "PT7H30M"
                },
                "2019-01-02/2019-01-02": {
                  "etablertTilsynTimerPerDag": "PT7H30M"
                },
                "2019-01-03/2019-01-09": {
                  "etablertTilsynTimerPerDag": "PT7H30M"
                }
              }
            },
            "arbeidstid": {
              "arbeidstakerList": [
                {
                  "norskIdentitetsnummer": null,
                  "organisasjonsnummer": "999999999",
                  "arbeidstidInfo": {
                    "perioder": {
                      "2018-12-30/2019-10-20": {
                        "jobberNormaltTimerPerDag": "PT7H30M",
                        "faktiskArbeidTimerPerDag": "PT7H30M"
                      }
                    }
                  }
                }
              ],
              "frilanserArbeidstidInfo": null,
              "selvstendigNæringsdrivendeArbeidstidInfo": null
            },
            "uttak": {
              "perioder": {
                "2018-12-30/2019-10-20": {
                  "timerPleieAvBarnetPerDag": "PT7H30M"
                }
              }
            },
            "omsorg": {
              "relasjonTilBarnet": "MOR",
              "beskrivelseAvOmsorgsrollen": "Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ."
            },
            "lovbestemtFerie": {
              "perioder": {
                "2019-02-21/2019-10-20": {
                  
                }
              }
            },
            "bosteder": {
              "perioder": {
                "2018-12-30/2019-10-20": {
                  "land": "GGY"
                }
              }
            },
            "utenlandsopphold": {
              "perioder": {
                "2018-12-30/2019-10-20": {
                  "land": "DNK",
                  "årsak": "barnetInnlagtIHelseinstitusjonForNorskOffentligRegning"
                },
                "2019-10-21/2020-01-01": {
                  "land": "ITA",
                  "årsak": "barnetInnlagtIHelseinstitusjonDekketEtterAvtaleMedEtAnnetLandOmTrygd"
                }
              }
            }
          }
        }
        """.trimIndent()

        @Language("JSON")
        private val forventetEtter = """
        {
            "søknadId": "1",
            "mottatt": "2020-10-12T12:53:21.046Z",
            "søker": {
                "identitetsnummer": "11111111111"
            },
            "journalposter": [{
                "inneholderInformasjonSomIkkeKanPunsjes": false,
                "inneholderMedisinskeOpplysninger": false,
                "journalpostId": "sajhdasd83724234"
            }],
            "pleiepengerSyktBarn": {
                "søknadsperioder": ["2018-12-30/2019-10-20"],
                "endringsperioder": [],
                "overordnet": {
                    "harForståttRettigheterOgPlikter": true,
                    "harBekreftetOpplysninger": true,
                    "samtidigHjemme": false,
                    "harMedsøker": false,
                    "bekrefterPeriodeOver8Uker": true
                },
                "barn": {
                    "identitetsnummer": "22111111111",
                    "fødselsdato": null
                },
                "arbeid": {
                    "selvstendigNæringsdrivende": [{
                        "perioder": {
                            "2018-11-11/2018-11-30": {
                                "virksomhetstyper": ["FISKE"]
                            }
                        },
                        "virksomhetsnavn": "Test"
                    }],
                    "frilanser": {
                        "startdato": "2019-10-10",
                        "jobberFortsattSomFrilanser": true
                    }
                },
                "beredskap": {
                    "perioder": {
                        "2019-02-21/2019-05-21": {
                            "tilleggsinformasjon": "Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ."
                        },
                        "2018-12-30/2019-02-20": {
                            "tilleggsinformasjon": "Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ."
                        }
                    }
                },
                "nattevåk": {
                    "perioder": {
                        "2019-02-21/2019-05-21": {
                            "tilleggsinformasjon": "Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ."
                        },
                        "2018-12-30/2019-02-20": {
                            "tilleggsinformasjon": "Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ."
                        }
                    }
                },
                "tilsynsordning": {
                    "perioder": {
                        "2019-01-01/2019-01-01": {
                            "etablertTilsynPerDag": "PT7H30M"
                        },
                        "2019-01-02/2019-01-02": {
                            "etablertTilsynPerDag": "PT7H30M"
                        },
                        "2019-01-03/2019-01-09": {
                            "etablertTilsynPerDag": "PT7H30M"
                        }
                    }
                },
                "arbeidstid": {
                    "arbeidstakere": [{
                        "identitetsnummer": null,
                        "organisasjonsnummer": "999999999",
                        "arbeidstid": {
                            "perioder": {
                                "2018-12-30/2019-10-20": {
                                    "normalArbeidstidPerDag": "PT7H30M",
                                    "faktiskArbeidstidPerDag": "PT7H30M"
                                }
                            }
                        }
                    }],
                    "frilanser": null,
                    "selvstendigNæringsdrivende": null
                },
                "omsorg": {
                    "relasjonTilBarnet": "MOR",
                    "beskrivelseAvOmsorgsrollen": "Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ."
                },
                "lovbestemtFerie": {
                    "perioder": {
                        "2019-02-21/2019-10-20": {}
                    }
                },
                "bosteder": {
                    "perioder": {
                        "2018-12-30/2019-10-20": {
                            "land": "Guernsey"
                        }
                    }
                },
                "utenlandsopphold": {
                    "perioder": {
                        "2018-12-30/2019-10-20": {
                            "land": "Danmark",
                            "årsak": "Barnet innlagt på helseinstitusjon for norsk offentelig regning"
                        },
                        "2019-10-21/2020-01-01": {
                            "land": "Italia",
                            "årsak": "Barnet innlagt på helseinstitusjon dekket etter avtale med annet land"
                        }
                    }
                }
            }
        }
        """.trimIndent()
    }
}