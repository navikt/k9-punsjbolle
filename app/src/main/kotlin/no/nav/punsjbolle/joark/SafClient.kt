package no.nav.punsjbolle.joark

import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.punsjbolle.AzureAwareClient
import no.nav.punsjbolle.JournalpostId
import java.net.URI

internal class SafClient(
    baseUrl: URI,
    accessTokenClient: AccessTokenClient,
    scopes: Set<String>
) : AzureAwareClient(
    navn = "SafClient",
    accessTokenClient = accessTokenClient,
    scopes = scopes,
    pingUrl = URI("$baseUrl/isReady")) {

    internal fun hentJournalposter(journalpostIder: Set<JournalpostId>) : Set<Journalpost> {
        // TODO: Legge til integrasjon mot SAF for å hente info
        return emptySet()
    }
}