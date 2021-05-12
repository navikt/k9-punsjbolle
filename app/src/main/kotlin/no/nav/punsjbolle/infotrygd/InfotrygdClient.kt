package no.nav.punsjbolle.infotrygd

import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.punsjbolle.AzureAwareClient
import no.nav.punsjbolle.CorrelationId
import no.nav.punsjbolle.Identitetsnummer
import no.nav.punsjbolle.ruting.RutingGrunnlag
import java.net.URI
import java.time.LocalDate

// TODO: Lage PR i azure-iac & allowliste tjenesten
internal class InfotrygdClient(
    baseUrl: URI,
    accessTokenClient: AccessTokenClient,
    scopes: Set<String>
) : AzureAwareClient(
    navn = "InfotrygdClient",
    accessTokenClient = accessTokenClient,
    scopes = scopes,
    pingUrl = URI("$baseUrl/isReady")) { // TODO: Finne rette link

    internal suspend fun harLøpendeSakSomInvolverer(
        fraOgMed: LocalDate,
        søker: Identitetsnummer,
        pleietrengende: Identitetsnummer?,
        annenPart: Identitetsnummer?,
        correlationId: CorrelationId
    ): RutingGrunnlag { // TODO Legge til tilsvarende som https://github.com/navikt/k9-fordel/blob/master/integrasjon/infotrygd-paaroerende-sykdom/src/main/java/no/nav/k9/integrasjon/infotrygdpaaroerendesykdom/RestInfotrygdP%C3%A5r%C3%B8rendeSykdomService.java#L11
        return RutingGrunnlag(
            søker = false,
            pleietrengende = false,
            annenPart = false
        )
    }
}