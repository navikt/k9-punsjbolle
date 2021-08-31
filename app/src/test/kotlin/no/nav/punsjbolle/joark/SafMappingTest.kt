package no.nav.punsjbolle.joark

import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.joark.SafClient.Companion.somJournalpost
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals

internal class SafMappingTest {

    private val journalpostId = "12341234".somJournalpostId()
    private val datoOpprettet = "2018-01-01T12:00:00"
    private val opprettet = LocalDateTime.parse(datoOpprettet)

    @Test
    fun `minimum data fra joark`() {
        @Language("JSON")
        val response = JSONObject("""
            {
               "journalposttype": "RAR",
               "journalstatus": "OK",
               "datoOpprettet": "$datoOpprettet",
               "dokumenter": [{ "brevkode": null }]
            }
        """.trimIndent())

        val forventet = Journalpost(
            journalpostId = journalpostId,
            journalposttype = "RAR",
            journalpoststatus = "OK",
            opprettet = opprettet,
            eksternReferanse = null,
            brevkode = null,
            sak = null
        )

        assertEquals(forventet, response.somJournalpost(journalpostId))
    }

    @Test
    fun `all data fra joark`() {
        @Language("JSON")
        val response = JSONObject("""
            {
               "journalposttype": "RAR2",
               "journalstatus": "OK2",
               "datoOpprettet": "$datoOpprettet",
               "dokumenter": [{ "brevkode": null }, { "brevkode": "Brevkoden" }, { "brevkode": "Brevkoden2" }],
               "eksternReferanseId": "Ekstern123",
               "sak": {
                  "fagsaksystem": "K9",
                  "fagsakId": "SAK123"
               }
            }
        """.trimIndent())

        val forventet = Journalpost(
            journalpostId = journalpostId,
            journalposttype = "RAR2",
            journalpoststatus = "OK2",
            opprettet = opprettet,
            eksternReferanse = "Ekstern123",
            brevkode = "Brevkoden",
            sak = Journalpost.Sak(
                fagsaksystem = "K9",
                fagsakId = "SAK123"
            )
        )

        assertEquals(forventet, response.somJournalpost(journalpostId))
    }

    @Test
    fun `har fagsaksystem men ikke fagsakid`() {
        @Language("JSON")
        val response = JSONObject("""
            {
               "journalposttype": "RAR2",
               "journalstatus": "OK2",
               "datoOpprettet": "$datoOpprettet",
               "dokumenter": [{ "brevkode": null }, { "brevkode": "Brevkoden" }, { "brevkode": "Brevkoden2" }],
               "eksternReferanseId": "Ekstern123",
               "sak": {
                  "fagsaksystem": "K9",
                  "fagsakId": null
               }
            }
        """.trimIndent())

        val forventet = Journalpost(
            journalpostId = journalpostId,
            journalposttype = "RAR2",
            journalpoststatus = "OK2",
            opprettet = opprettet,
            eksternReferanse = "Ekstern123",
            brevkode = "Brevkoden",
            sak = null
        )

        assertEquals(forventet, response.somJournalpost(journalpostId))
    }

    @Test
    fun `har fagsakid men ikke fagsaksystem`() {
        @Language("JSON")
        val response = JSONObject("""
            {
               "journalposttype": "RAR2",
               "journalstatus": "OK2",
               "datoOpprettet": "$datoOpprettet",
               "dokumenter": [{ "brevkode": null }, { "brevkode": "Brevkoden" }, { "brevkode": "Brevkoden2" }],
               "eksternReferanseId": "Ekstern123",
               "sak": {
                  "fagsaksystem": null,
                  "fagsakId": "SAK3"
               }
            }
        """.trimIndent())

        val forventet = Journalpost(
            journalpostId = journalpostId,
            journalposttype = "RAR2",
            journalpoststatus = "OK2",
            opprettet = opprettet,
            eksternReferanse = "Ekstern123",
            brevkode = "Brevkoden",
            sak = null
        )

        assertEquals(forventet, response.somJournalpost(journalpostId))
    }
}