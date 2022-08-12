package no.nav.punsjbolle.testutils.søknad

import de.huxhorn.sulky.ulid.ULID
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import org.json.JSONObject
import java.util.*

internal object PunsjetSøknadVerktøy {
    private val ulid = ULID()

    internal fun punsjetSøknad(søknad: JSONObject, brevkode: Brevkode = Brevkode.PLEIEPENGER_BARN_SOKNAD) = Behovssekvens(
        id = ulid.nextULID(),
        correlationId = "${UUID.randomUUID()}",
        behov = arrayOf(Behov(
            navn = "PunsjetSøknad",
            input = mapOf(
                "versjon" to "1.0.0",
                "søknad" to søknad.toMap(),
                "søknadstype" to brevkode
            )
        ))
    ).keyValue.second
}
