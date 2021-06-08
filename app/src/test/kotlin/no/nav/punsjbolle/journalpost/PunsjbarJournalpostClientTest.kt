package no.nav.punsjbolle.journalpost

import no.nav.punsjbolle.AktørId.Companion.somAktørId
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.Søknadstype
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert

internal class PunsjbarJournalpostClientTest {

    @Test
    fun `mapper kopiert journalpost`() {

        val kopiertJournalpost = KopiertJournalpost(
            journalpostId = "22222222222".somJournalpostId(),
            opprinneligJournalpostId = "11111111111".somJournalpostId(),
            aktørId = "33333333333".somAktørId(),
            søknadstype = Søknadstype.PleiepengerSyktBarn
        )

        val (key, value) = PunsjbarJournalpostClient.map(kopiertJournalpost)
        assertEquals("22222222222",key)

        @Language("JSON")
        val forventetValue = """
        {
            "journalpostId": "22222222222",
            "aktørId": "33333333333",
            "ytelse": "PSB",
            "type": "KOPI",
            "opprinneligJournalpost": {
                "journalpostId": "11111111111"
            }
        }
        """.trimIndent()

        JSONAssert.assertEquals(forventetValue, value, true)
    }

    @Test
    fun `feiler ved overstyring av nøkkelfelt`() {
        assertThrows<IllegalArgumentException> {
            PunsjbarJournalpostClient.map(object : PunsjbarJournalpost {
                override val journalpostId = "1111111111".somJournalpostId()
                override val aktørId = "33333333333".somAktørId()
                override val søknadstype = Søknadstype.PleiepengerSyktBarn
                override val type = "TEST"
                override val ekstraInfo = mapOf("journalpostId" to mapOf(
                    "foo" to "bar"
                ))
            })
        }
    }
}