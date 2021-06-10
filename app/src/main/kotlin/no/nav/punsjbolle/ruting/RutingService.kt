package no.nav.punsjbolle.ruting

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.punsjbolle.AktørId
import no.nav.punsjbolle.CorrelationId
import no.nav.punsjbolle.Identitetsnummer
import no.nav.punsjbolle.Søknadstype
import no.nav.punsjbolle.infotrygd.InfotrygdClient
import no.nav.punsjbolle.k9sak.K9SakClient
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate

internal class RutingService(
    private val k9SakClient: K9SakClient,
    private val infotrygdClient: InfotrygdClient) {

    private val cache: Cache<DestinasjonInput, Destinasjon> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(2))
        .maximumSize(100)
        .build()

    internal suspend fun destinasjon(
        søker: Identitetsnummer,
        fraOgMed: LocalDate,
        pleietrengende: Identitetsnummer? = null,
        annenPart: Identitetsnummer? = null,
        søknadstype: Søknadstype,
        aktørIder: Set<AktørId>,
        correlationId: CorrelationId) : Destinasjon {

        val input = DestinasjonInput(
            søker = søker,
            fraOgMed = fraOgMed,
            pleietrengende = pleietrengende,
            annenPart = annenPart,
            søknadstype = søknadstype,
            aktørIder = aktørIder
        )

        return when (val cacheValue = cache.getIfPresent(input)) {
            null -> slåOppDestinasjon(
                input = input,
                correlationId = correlationId
            ).also { cache.put(input, it) }
            else -> cacheValue.also {
                logger.info("Rutes til ${it.name}, oppslaget finnes i cache.")
            }
        }
    }

    private suspend fun slåOppDestinasjon(
        input: DestinasjonInput,
        correlationId: CorrelationId) : Destinasjon {
        if (k9SakClient.inngårIUnntaksliste(aktørIder = input.aktørIder, søknadstype = input.søknadstype, correlationId = correlationId)) {
            logger.info("Rutes til Infotrygd ettersom minst en part er lagt til i unntakslisten i K9Sak.")
            return Destinasjon.Infotrygd
        }

        val k9SakGrunnlag = k9SakClient.harLøpendeSakSomInvolvererEnAv(
            søker = input.søker,
            fraOgMed = input.fraOgMed,
            pleietrengende = input.pleietrengende,
            annenPart = input.annenPart,
            søknadstype = input.søknadstype,
            correlationId = correlationId
        )

        if (k9SakGrunnlag.minstEnPart) {
            logger.info("Rutes til K9Sak ettersom minst en part er involvert i løpende sak. K9Sak=[$k9SakGrunnlag]")
            return Destinasjon.K9Sak
        }

        val infotrygdGrunnlag = infotrygdClient.harLøpendeSakSomInvolvererEnAv(
            søker = input.søker,
            fraOgMed = input.fraOgMed.minusYears(2),
            pleietrengende = input.pleietrengende,
            annenPart = input.annenPart,
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

        private data class DestinasjonInput(
            val søker: Identitetsnummer,
            val fraOgMed: LocalDate,
            val pleietrengende: Identitetsnummer?,
            val annenPart: Identitetsnummer?,
            val søknadstype: Søknadstype,
            val aktørIder: Set<AktørId>
        )
    }
}

