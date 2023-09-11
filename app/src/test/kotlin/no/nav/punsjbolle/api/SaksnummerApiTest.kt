package no.nav.punsjbolle.api

import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.clearMocks
import io.mockk.mockk
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.punsjbolle.AktørId.Companion.somAktørId
import no.nav.punsjbolle.ApplicationContext
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.punsjbolle.Periode
import no.nav.punsjbolle.joark.SafClient
import no.nav.punsjbolle.k9sak.K9SakClient
import no.nav.punsjbolle.punsjbolle
import no.nav.punsjbolle.sak.SakClient
import no.nav.punsjbolle.testutils.ApplicationContextExtension
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.URI
import java.util.*

@ExtendWith(ApplicationContextExtension::class)
internal class SaksnummerApiTest(
    builder: ApplicationContext.Builder
) {

    private val safClientMock: SafClient = mockk()
    private val k9SakClientMock: K9SakClient = mockk()
    private val sakClientMock: SakClient = mockk()

    private val applicationContext = builder.also { builder ->
        builder.safClient = safClientMock
        builder.k9SakClient = k9SakClientMock
        builder.sakClient = sakClientMock
    }.build()


    @BeforeEach
    fun beforeEach() {
        clearMocks(safClientMock, k9SakClientMock, sakClientMock)
    }

    @Test
    fun `Ingen authorization header`() {
        val (httpStatus, k9Saksnummer) = requestSaksnummer(jwt = null)
        assertEquals(HttpStatusCode.Unauthorized, httpStatus)
        assertNull(k9Saksnummer)
    }

    @Test
    fun `Feil audience`() {
        val (httpStatus, k9Saksnummer) = requestSaksnummer(
            jwt = Azure.V2_0.generateJwt(
                clientId = "foo",
                audience = "k9-sak"
            )
        )
        assertEquals(HttpStatusCode.Forbidden, httpStatus)
        assertNull(k9Saksnummer)
    }

    @Test
    fun `Feil issuer`() {
        val (httpStatus, k9Saksnummer) = requestSaksnummer(
            jwt = Azure.V1_0.generateJwt(
                clientId = "foo",
                audience = "k9-punsjbolle"
            )
        )
        assertEquals(HttpStatusCode.Unauthorized, httpStatus)
        assertNull(k9Saksnummer)
    }

    private fun requestSaksnummer(
        periode: Periode? = null,
        journalpostId: JournalpostId? = benyttetJournalpostId,
        jwt: String? = Azure.V2_0.generateJwt(
            clientId = "foo",
            audience = "k9-punsjbolle"
        )
    ) = withTestApplication({ punsjbolle(applicationContext) }) {
        handleRequest(HttpMethod.Post, "/api/saksnummer") {
            addHeader(HttpHeaders.XCorrelationId, "${UUID.randomUUID()}")
            addHeader(HttpHeaders.ContentType, "application/json")
            jwt?.let { addHeader(HttpHeaders.Authorization, "Bearer $it") }
            setBody(requestBody(periode = periode, journalpostId = journalpostId))
        }.let {
            val status = it.response.status()!!
            val saksnummerEllerErrorType: Any? = when (status) {
                HttpStatusCode.OK -> JSONObject(it.response.content!!).getString("saksnummer").somK9Saksnummer()
                HttpStatusCode.Conflict -> URI(JSONObject(it.response.content!!).getString("type"))
                else -> null
            }
            status to saksnummerEllerErrorType
        }
    }


    private companion object {
        private val søkerIdentitetsnummer = "11111111111".somIdentitetsnummer()
        private val søkerAktørId = "22222222222".somAktørId()
        private val pleietrengendeIdentitetsnummer = "33333333333".somIdentitetsnummer()
        private val pleietrengendeAktørId = "44444444444".somAktørId()
        private val benyttetJournalpostId = "55555555555".somJournalpostId()

        @Language("JSON")
        private fun requestBody(periode: Periode?, journalpostId: JournalpostId?) = """
            {
              "søker": {
                "identitetsnummer": "$søkerIdentitetsnummer",
                "aktørId": "$søkerAktørId"
              },
              "pleietrengende": {
                "identitetsnummer": "$pleietrengendeIdentitetsnummer",
                "aktørId": "$pleietrengendeAktørId"
              },
              "søknadstype": "PleiepengerSyktBarn",
              "journalpostId": ${journalpostId?.let { """"$it"""" }},
              "periode": ${periode?.let { """"$it"""" }}
            }
        """.trimIndent()
    }
}
