package no.nav.punsjbolle.ruting

import no.nav.punsjbolle.AktørId
import no.nav.punsjbolle.CorrelationId
import no.nav.punsjbolle.Identitetsnummer
import no.nav.punsjbolle.infotrygd.InfotrygdClient
import no.nav.punsjbolle.k9sak.K9SakClient
import java.time.LocalDate

internal class RutingService(
    private val k9SakClient: K9SakClient,
    private val infotrygdClient: InfotrygdClient) {

    internal suspend fun destinasjon(
        søker: Pair<Identitetsnummer, AktørId>,
        fraOgMed: LocalDate,
        pleietrengende: Pair<Identitetsnummer, AktørId>? = null,
        annenPart: Pair<Identitetsnummer, AktørId>? = null,
        correlationId: CorrelationId) : Destinasjon {

        val k9SakGrunnlag = k9SakClient.harLøpendeSakSomInvolverer(
            søker = søker.second,
            fraOgMed = fraOgMed,
            pleietrengende = pleietrengende?.second,
            annenPart = annenPart?.second,
            correlationId = correlationId
        )

        val infotrygdGrunnlag = infotrygdClient.harLøpendeSakSomInvolverer(
            søker = søker.first,
            fraOgMed = fraOgMed,
            pleietrengende = pleietrengende?.first,
            annenPart = annenPart?.first,
            correlationId = correlationId
        )

        return when {
            k9SakGrunnlag.minstEnPart && infotrygdGrunnlag.minstEnPart -> throw IllegalStateException(
                "Berører parter som finnes både i K9Sak og Infotrygd. K9Sak=[$k9SakGrunnlag], Infotrygd=[$infotrygdGrunnlag]"
            )
            infotrygdGrunnlag.minstEnPart -> Destinasjon.Infotrygd
            else -> Destinasjon.K9Sak
        }
    }

    internal enum class Destinasjon {
        K9Sak,
        Infotrygd
    }
}

