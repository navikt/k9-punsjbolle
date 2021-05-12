package no.nav.punsjbolle.infotrygd

import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.punsjbolle.AzureAwareClient
import no.nav.punsjbolle.CorrelationId
import no.nav.punsjbolle.Identitetsnummer
import no.nav.punsjbolle.ruting.RutingGrunnlag
import java.net.URI
import java.time.LocalDate

internal class InfotrygdClient(
    baseUrl: URI,
    accessTokenClient: AccessTokenClient,
    scopes: Set<String>
) : AzureAwareClient(
    navn = "InfotrygdClient",
    accessTokenClient = accessTokenClient,
    scopes = scopes,
    pingUrl = URI("$baseUrl/isReady")) { // TODO

    internal suspend fun harLøpendeSakSomInvolverer(
        fraOgMed: LocalDate,
        søker: Identitetsnummer,
        pleietrengende: Identitetsnummer?,
        annenPart: Identitetsnummer?,
        correlationId: CorrelationId
    ): RutingGrunnlag {
        return RutingGrunnlag(
            søker = false,
            pleietrengende = false,
            annenPart = false
        )
    }
}