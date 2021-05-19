package no.nav.punsjbolle.ruting

import no.nav.punsjbolle.CorrelationId
import no.nav.punsjbolle.Identitetsnummer
import no.nav.punsjbolle.Søknadstype
import no.nav.punsjbolle.infotrygd.InfotrygdClient
import no.nav.punsjbolle.k9sak.K9SakClient
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class RutingService(
    private val k9SakClient: K9SakClient,
    private val infotrygdClient: InfotrygdClient) {

    internal suspend fun destinasjon(
        søker: Identitetsnummer,
        fraOgMed: LocalDate,
        pleietrengende: Identitetsnummer? = null,
        annenPart: Identitetsnummer? = null,
        søknadstype: Søknadstype,
        correlationId: CorrelationId) : Destinasjon {

        val k9SakGrunnlag = k9SakClient.harLøpendeSakSomInvolvererEnAv(
            søker = søker,
            fraOgMed = fraOgMed,
            pleietrengende = pleietrengende,
            annenPart = annenPart,
            søknadstype = søknadstype,
            correlationId = correlationId
        )

        val infotrygdGrunnlag = infotrygdClient.harLøpendeSakSomInvolvererEnAv(
            søker = søker,
            fraOgMed = fraOgMed,
            pleietrengende = pleietrengende,
            annenPart = annenPart,
            correlationId = correlationId
        )

        return when {
            k9SakGrunnlag.minstEnPart && infotrygdGrunnlag.minstEnPart -> logger.warn(
                "Berører parter som finnes både i Infotrygd og K9Sak. Infotrygd=[$infotrygdGrunnlag], K9Sak=[$k9SakGrunnlag]"
            ).let { Destinasjon.K9Sak }
            infotrygdGrunnlag.minstEnPart -> Destinasjon.Infotrygd
            else -> Destinasjon.K9Sak
        }
    }

    internal enum class Destinasjon {
        K9Sak,
        Infotrygd
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(RutingService::class.java)
    }
}

