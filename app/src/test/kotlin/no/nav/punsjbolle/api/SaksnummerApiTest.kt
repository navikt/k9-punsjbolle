package no.nav.punsjbolle.api

import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.punsjbolle.AktørId.Companion.somAktørId
import no.nav.punsjbolle.ApplicationContext
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.punsjbolle.infotrygd.InfotrygdClient
import no.nav.punsjbolle.joark.Journalpost
import no.nav.punsjbolle.joark.SafClient
import no.nav.punsjbolle.k9sak.K9SakClient
import no.nav.punsjbolle.punsjbolle
import no.nav.punsjbolle.ruting.RutingGrunnlag
import no.nav.punsjbolle.testutils.ApplicationContextExtension
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(ApplicationContextExtension::class)
internal class SaksnummerApiTest(
    builder: ApplicationContext.Builder) {

    private val safClientMock : SafClient = mockk()
    private val k9SakClientMock : K9SakClient = mockk()
    private val infotrygdClientMock : InfotrygdClient = mockk()

    private val applicationContext = builder.also { builder ->
        builder.safClient = safClientMock
        builder.k9SakClient = k9SakClientMock
        builder.infotrygdClient = infotrygdClientMock
    }.build()


    @BeforeEach
    fun beforeEach() {
        clearMocks(safClientMock, k9SakClientMock, infotrygdClientMock)
    }

    @Test
    fun `ingen saker i hverken Infotrygd eller K9sak med journalpostId`() {
        mockInfotrygd()
        mockK9Sak()
        mockHentSaksnummer()
        mockHentJournalpost()

        val (httpStatus, k9Saksnummer) = request(journalpostId = journalpostId, fraOgMed = null)
        assertEquals(HttpStatusCode.OK, httpStatus)
        assertEquals(saksnummer, k9Saksnummer)
        verifiserSafKalt()
    }

    @Test
    fun `ingen saker i hverken Infotrygd eller K9sak med fraOgMed`() {
        mockInfotrygd()
        mockK9Sak()
        mockHentSaksnummer()

        val (httpStatus, k9Saksnummer) = request(journalpostId = null, fraOgMed = LocalDate.now())
        assertEquals(HttpStatusCode.OK, httpStatus)
        assertEquals(saksnummer, k9Saksnummer)
        verifiserSafIkkeKalt()
    }

    @Test
    fun `søker har sak i infotrygd, ingenting i K9Sak`() {
        mockInfotrygd(søker = true)
        mockK9Sak()
        mockHentJournalpost()

        val (httpStatus, k9Saksnummer) = request(journalpostId = journalpostId, fraOgMed = null)
        assertEquals(HttpStatusCode.Conflict, httpStatus)
        assertNull(k9Saksnummer)
        verifiserSafKalt()
        verifiserSaksnummerIkkeOpprettet()
    }

    @Test
    fun `pleietrengende har vedtak i Infotrygd, ingenting i K9Sak`() {
        mockInfotrygd(pleietrengende = true)
        mockK9Sak()
        mockHentJournalpost()

        val (httpStatus, k9Saksnummer) = request(journalpostId = journalpostId, fraOgMed = null)
        assertEquals(HttpStatusCode.Conflict, httpStatus)
        assertNull(k9Saksnummer)
        verifiserSafKalt()
        verifiserSaksnummerIkkeOpprettet()
    }

    @Test
    fun `søker har sak i K9Sak, pleietrengende i Infotrygd`() {
        mockInfotrygd(pleietrengende = true)
        mockK9Sak(søker = true)
        mockHentSaksnummer()

        val (httpStatus, k9Saksnummer) = request(journalpostId = null, fraOgMed = LocalDate.now())
        assertEquals(HttpStatusCode.OK, httpStatus)
        assertEquals(saksnummer, k9Saksnummer)
        verifiserSafIkkeKalt()
    }

    @Test
    fun `Ingen authorization header`() {
        val (httpStatus, k9Saksnummer) = request(journalpostId = null, fraOgMed = LocalDate.now(), jwt = null)
        assertEquals(HttpStatusCode.Unauthorized, httpStatus)
        assertNull(k9Saksnummer)
    }

    @Test
    fun `Feil audience`() {
        val (httpStatus, k9Saksnummer) = request(journalpostId = null, fraOgMed = LocalDate.now(), jwt = Azure.V2_0.generateJwt(
            clientId = "foo",
            audience = "k9-sak"
        ))
        assertEquals(HttpStatusCode.Forbidden, httpStatus)
        assertNull(k9Saksnummer)
    }

    @Test
    fun `Feil issuer`() {
        val (httpStatus, k9Saksnummer) = request(journalpostId = null, fraOgMed = LocalDate.now(), jwt = Azure.V1_0.generateJwt(
            clientId = "foo",
            audience = "k9-punsjbolle"
        ))
        assertEquals(HttpStatusCode.Unauthorized, httpStatus)
        assertNull(k9Saksnummer)
    }

    private fun request(
        fraOgMed: LocalDate?,
        journalpostId: JournalpostId?,
        jwt: String? = Azure.V2_0.generateJwt(
            clientId = "foo",
            audience = "k9-punsjbolle"
        )) = withTestApplication( { punsjbolle(applicationContext)}) {
        handleRequest(HttpMethod.Post, "/api/saksnummer") {
            addHeader(HttpHeaders.XCorrelationId, "${UUID.randomUUID()}")
            addHeader(HttpHeaders.ContentType, "application/json; charset=UTF-8")
            jwt?.let { addHeader(HttpHeaders.Authorization, "Bearer $it") }
            setBody(requestBody(fraOgMed = fraOgMed, journalpostId = journalpostId))
        }.let {
            val status = it.response.status()!!
            val saksnummer = when (status == HttpStatusCode.OK) {
                true -> JSONObject(it.response.content!!).getString("saksnummer").somK9Saksnummer()
                false -> null
            }
            status to saksnummer
        }
    }

    private fun verifiserSafIkkeKalt() =
        coVerify { safClientMock.hentJournalpost(any(), any()) wasNot Called }

    private fun verifiserSafKalt() =
        coVerify(exactly = 1) { safClientMock.hentJournalpost(any(), any()) }

    private fun verifiserSaksnummerIkkeOpprettet() =
        coVerify { k9SakClientMock.hentSaksnummer(any(), any()) wasNot Called }

    private fun mockInfotrygd(søker: Boolean = false, pleietrengende: Boolean = false, annenPart: Boolean = false) {
        coEvery { infotrygdClientMock.harLøpendeSakSomInvolvererEnAv(any(),any(), any(), any(), any()) }.returns(
            RutingGrunnlag(søker = søker, pleietrengende = pleietrengende, annenPart = annenPart)
        )
    }

    private fun mockK9Sak(søker: Boolean = false, pleietrengende: Boolean = false, annenPart: Boolean = false) {
        coEvery { k9SakClientMock.harLøpendeSakSomInvolvererEnAv(any(),any(), any(), any(), any(), any()) }.returns(
            RutingGrunnlag(søker = søker, pleietrengende = pleietrengende, annenPart = annenPart)
        )
    }

    private fun mockHentSaksnummer() {
        coEvery { k9SakClientMock.hentSaksnummer(any(),any()) }.returns(saksnummer)
    }

    private fun mockHentJournalpost() {
        coEvery { safClientMock.hentJournalpost(any(), any()) }.returns(Journalpost(
            journalpostId = journalpostId,
            journalposttype = "foo",
            journalpoststatus = "bar",
            opprettet = opprettet.atStartOfDay(),
            eksternReferanse = null,
            brevkode = null,
            sak = null
        ))
    }


    private companion object {
        private val opprettet = LocalDate.parse("2021-01-01")
        private val saksnummer = "SAK123".somK9Saksnummer()
        private val søkerIdentitetsnummer = "11111111111".somIdentitetsnummer()
        private val søkerAktørId = "22222222222".somAktørId()
        private val pleietrengendeIdentitetsnummer = "33333333333".somIdentitetsnummer()
        private val pleietrengendeAktørId = "44444444444".somAktørId()
        private val journalpostId = "55555555555".somJournalpostId()

        @Language("JSON")
        private fun requestBody(fraOgMed: LocalDate?, journalpostId: JournalpostId?) = """
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
              "fraOgMed": ${fraOgMed?.let { """"$it"""" }}
            }
        """.trimIndent()
    }
}