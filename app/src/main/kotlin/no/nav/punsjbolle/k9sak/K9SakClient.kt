package no.nav.punsjbolle.k9sak

import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.punsjbolle.AzureAwareClient
import no.nav.punsjbolle.CorrelationId
import no.nav.punsjbolle.K9Saksnummer
import no.nav.punsjbolle.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.punsjbolle.joark.Journalpost
import no.nav.punsjbolle.meldinger.HentK9SaksnummerMelding
import no.nav.punsjbolle.søknad.PunsjetSøknadMelding
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.*

internal class K9SakClient(
    baseUrl: URI,
    accessTokenClient: AccessTokenClient,
    scopes: Set<String>
) : AzureAwareClient(
    navn = "K9SakClient",
    accessTokenClient = accessTokenClient,
    scopes = scopes,
    pingUrl = URI("$baseUrl/internal/health/isReady")) {

    internal fun hentSaksnummer(
        grunnlag: HentK9SaksnummerMelding.HentK9SaksnummerGrunnlag,
        correlationId: CorrelationId) : K9Saksnummer {

        val dto = """
            {
                "ytelseType": "${grunnlag.søknadstype.k9SakDto}",
                "aktørId": "$grunnlag.søker",
                "pleietrengendeAktørId": ${grunnlag.pleietrengende?.let { "$it" }},
                "relatertPersonAktørId": "${grunnlag.annenPart?.let { "$it" }}",
                "periode": {
                    "fom": "$grunnlag.fraOgMed",
                    "tom": ${grunnlag.tilOgMed?.let { "$it" }}
                }
            }
        """.trimIndent()

        logger.info("hentSaksnummerDto=[$dto], correlationId=[$correlationId]")
        // TODO: Legge til integrasjon mot K9sak
        return "TODO123".somK9Saksnummer()
    }

    internal fun sendInnSøknad(
        søknad: PunsjetSøknadMelding.PunsjetSøknad,
        saksnummer: K9Saksnummer,
        journalpost: Journalpost,
        correlationId: CorrelationId) {

        val dto = """
            [{
                "saksnummer": "$saksnummer",
                "journalpostId": "${journalpost.journalpostId}",
                "ytelseType": {
                    "kode": "${søknad.søknadstype.k9SakDto}"
                },
                "kanalReferanse": "${journalpost.kanalReferanse()}",
                "brevkode": "${journalpost.brevkode()}",
                "forsendelseMottattTidspunkt": "${journalpost.forsendelseTidspunkt}",
                "forsendelseMottatt": "${journalpost.forsendelseTidspunkt.toLocalDate()}",
                "base64EncodedPayload": "${Base64.getUrlEncoder().encodeToString(søknad.søknadJson.toString().toByteArray())}"
            }]
        """.trimIndent()

        //  TODO: Legge til integrasjon mot K9Sak
        logger.info("sendInnSøknadDto=[$dto], correlationId=[$correlationId]")
    }

    private companion object {
        private const val Punsjbolle = "Punsjbolle"
        private val logger = LoggerFactory.getLogger(K9SakClient::class.java)
        private fun Journalpost.brevkode() = when (brevkode) {
            null -> Punsjbolle.also { logger.warn("JournalpostId=[$journalpostId] mangler brevkode, defaulter til Brevkode=[$it]") }
            else -> brevkode
        }
        private fun Journalpost.kanalReferanse() = when (kanalReferanse) {
            null -> "$Punsjbolle-$journalpostId".also { logger.warn("JournalpostId=[$journalpostId] mangler kanalReferanse, setter KanalReferanse=[$it]") }
            else -> kanalReferanse
        }
    }
}