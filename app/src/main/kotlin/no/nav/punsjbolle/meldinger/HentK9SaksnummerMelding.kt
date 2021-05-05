package no.nav.punsjbolle.meldinger

import no.nav.k9.rapid.behov.Behov
import no.nav.punsjbolle.AktørId
import no.nav.punsjbolle.BehovMedLøsning
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.Søknadstype
import java.time.LocalDate

internal object HentK9SaksnummerMelding : BehovMedLøsning<HentK9SaksnummerMelding.HentK9SaksnummerGrunnlag, Pair<K9Saksnummer, HentK9SaksnummerMelding.K9SaksnummerKilde>> {

    private const val BehovNavn = "HentK9Saksnummer"

    internal data class HentK9SaksnummerGrunnlag(
        internal val søknadstype: Søknadstype,
        internal val søker: AktørId,
        internal val pleietrengende: AktørId?,
        internal val annenPart: AktørId?,
        internal val fraOgMed: LocalDate,
        internal val tilOgMed: LocalDate?
    )

    internal enum class K9SaksnummerKilde {
        ManueltValgtIPunsj,
        SlåttOppMotK9Sak
    }

    override fun behovMedLøsning(behovInput: HentK9SaksnummerGrunnlag, løsning: Pair<K9Saksnummer, K9SaksnummerKilde>): Pair<Behov, Map<String, *>> {
        return Behov(navn = BehovNavn, input = mapOf(
            "aktører" to mapOf(
                "søker" to "${behovInput.søker}",
                "pleietrengende" to behovInput.pleietrengende?.let { "$it" },
                "annenPart" to behovInput.annenPart?.let { "$it" }
            ),
            "periode" to mapOf(
                "fom" to "${behovInput.fraOgMed}",
                "tom" to behovInput.tilOgMed?.let { "$it" }
            ),
            "søknadstype" to behovInput.søknadstype.name
        )) to mapOf(
            "saksnummer" to "${løsning.first}",
            "kilde" to løsning.second.name
        )
    }
}