package no.nav.punsjbolle.joark

import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.punsjbolle.AzureAwareClient
import no.nav.punsjbolle.CorrelationId
import no.nav.punsjbolle.JournalpostId
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDateTime
import java.util.*

internal class SafClient(
    baseUrl: URI,
    accessTokenClient: AccessTokenClient,
    scopes: Set<String>
) : AzureAwareClient(
    navn = "SafClient",
    accessTokenClient = accessTokenClient,
    scopes = scopes,
    pingUrl = URI("$baseUrl/isReady")) {

    internal suspend fun hentJournalposter(
        journalpostIder: Set<JournalpostId>,
        correlationId: CorrelationId) : Set<Journalpost> {
        // TODO: Legge til integrasjon mot SAF for å hente info
        logger.info("Henter journalposter for JounralpostIder=$journalpostIder, CorrelationId=[$correlationId]")
        return journalpostIder.map { Journalpost(
            journalpostId = it,
            journalpoststatus = "MOTTATT",
            journalposttype = "I",
            kanalReferanse = "${UUID.randomUUID()}",
            brevkode = "MinBrevkode",
            forsendelseTidspunkt = LocalDateTime.now(),
            sak = null
        )}.toSet()
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(SafClient::class.java)
        private fun hentJournalpostQuery(journalpostId: JournalpostId) = """
            query {
              journalpost(journalpostId: "$journalpostId") {
                tema
                journalposttype
                journalstatus
                eksternReferanseId
                sak {
                    fagsakId
                    fagsaksystem
                }
                dokumenter {
                    brevkode
                }
            }
        """.trimIndent()
    }
}