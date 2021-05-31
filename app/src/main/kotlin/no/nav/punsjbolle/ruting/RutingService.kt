package no.nav.punsjbolle.ruting

import no.nav.punsjbolle.AktørId
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
        aktørIder: Set<AktørId>,
        correlationId: CorrelationId) : Destinasjon {

        if (k9SakClient.inngårIUnntaksliste(aktørIder = aktørIder, søknadstype = søknadstype, correlationId = correlationId)) {
            logger.info("Rutes til Infotrygd ettersom minst en part er lagt til i unntakslisten i K9Sak.")
            return Destinasjon.Infotrygd
        }

        val k9SakGrunnlag = k9SakClient.harLøpendeSakSomInvolvererEnAv(
            søker = søker,
            fraOgMed = fraOgMed,
            pleietrengende = pleietrengende,
            annenPart = annenPart,
            søknadstype = søknadstype,
            correlationId = correlationId
        )

        if (k9SakGrunnlag.minstEnPart) {
            logger.info("Rutes til K9Sak ettersom minst en part er involvert i løpende sak. K9Sak=[$k9SakGrunnlag]")
            return Destinasjon.K9Sak
        }

        val infotrygdGrunnlag = infotrygdClient.harLøpendeSakSomInvolvererEnAv(
            søker = søker,
            fraOgMed = fraOgMed,
            pleietrengende = pleietrengende,
            annenPart = annenPart,
            correlationId = correlationId
        )

        return when {
            infotrygdGrunnlag.minstEnPart -> Destinasjon.Infotrygd.also {
                logger.info("Rutes til Infotrygd ettersom minst en part er involvert i en løpende sak. Infotrygd=[$infotrygdGrunnlag]")
            }
            else -> Destinasjon.K9Sak.also {
                logger.info("Rutes til K9Sak ettersom ingen parter er involvert hverken i Infotrygd eller K9Sak fra før")
            }
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

