package no.nav.punsjbolle.infotrygd

import no.nav.punsjbolle.CorrelationId
import no.nav.punsjbolle.Identitetsnummer
import no.nav.punsjbolle.ruting.RutingGrunnlag
import java.time.LocalDate

internal class InfotrygdClient  {
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