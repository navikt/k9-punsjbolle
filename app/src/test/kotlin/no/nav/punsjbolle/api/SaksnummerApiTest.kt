package no.nav.punsjbolle.api

import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.punsjbolle.*
import no.nav.punsjbolle.AktørId.Companion.somAktørId
import no.nav.punsjbolle.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.punsjbolle.JournalpostId.Companion.somJournalpostId
import no.nav.punsjbolle.K9Saksnummer.Companion.somK9Saksnummer
import no.nav.punsjbolle.infotrygd.InfotrygdClient
import no.nav.punsjbolle.joark.Journalpost
import no.nav.punsjbolle.joark.SafClient
import no.nav.punsjbolle.k9sak.K9SakClient
import no.nav.punsjbolle.ruting.RutingGrunnlag
import no.nav.punsjbolle.sak.SakClient
import no.nav.punsjbolle.testutils.ApplicationContextExtension
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.URI
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
    private val sakClientMock : SakClient = mockk()

    private val applicationContext = builder.also { builder ->
        builder.safClient = safClientMock
        builder.k9SakClient = k9SakClientMock
        builder.infotrygdClient = infotrygdClientMock
        builder.sakClient = sakClientMock
    }.build()


    @BeforeEach
    fun beforeEach() {
        clearMocks(safClientMock, k9SakClientMock, infotrygdClientMock, sakClientMock)
    }

    @Test
    fun `ingen saker i hverken Infotrygd eller K9sak`() {
        mockInfotrygd()
        mockK9Sak()
        mockHentSaksnummer()
        mockHentJournalpost()
        mockForsikreSakskobling()

        val (httpStatus, k9Saksnummer) = request()
        assertEquals(HttpStatusCode.OK, httpStatus)
        assertEquals(saksnummer, k9Saksnummer)

        assertInfotrygdKalt(true)
        assertK9SakKalt(true)
    }

    @Test
    fun `søker har sak i infotrygd, ingenting i K9Sak`() {
        mockInfotrygd(søker = true)
        mockK9Sak()
        mockHentSaksnummer()
        mockHentJournalpost()
        mockForsikreSakskobling()

        val (httpStatus, errorType) = request()
        assertEquals(HttpStatusCode.Conflict, httpStatus)
        assertEquals(URI("punsjbolle://må-behandles-i-infotrygd"), errorType)

        assertInfotrygdKalt(true)
        assertK9SakKalt(true)
    }


    @Test
    fun `pleietrengende har vedtak i Infotrygd, ingenting i K9Sak`() {
        mockInfotrygd(pleietrengende = true)
        mockK9Sak()
        mockHentJournalpost()

        val (httpStatus, errorType) = request()
        assertEquals(HttpStatusCode.Conflict, httpStatus)
        assertEquals(URI("punsjbolle://må-behandles-i-infotrygd"), errorType)
        assertInfotrygdKalt(true)
        assertK9SakKalt(true)
    }

    @Test
    fun `søker har sak i K9Sak, pleietrengende i Infotrygd`() {
        mockInfotrygd(pleietrengende = true)
        mockK9Sak(søker = true)
        mockHentSaksnummer()
        mockHentJournalpost()
        mockForsikreSakskobling()

        val (httpStatus, k9Saksnummer) = request()
        assertEquals(HttpStatusCode.OK, httpStatus)
        assertEquals(saksnummer, k9Saksnummer)

        assertInfotrygdKalt(false)
        assertK9SakKalt(true)
    }

    @Test
    fun `journalpost knyttet til annet saksnummer i K9Sak`() {
        mockInfotrygd(pleietrengende = true)
        mockK9Sak(søker = true)
        mockHentSaksnummer()
        mockHentJournalpost(fagsaksystem = "K9", fagsakId = "EtAnnet")

        val (httpStatus, errorType) = request()
        assertEquals(HttpStatusCode.Conflict, httpStatus)
        assertEquals(URI("punsjbolle://ikke-støttet-journalpost"), errorType)
    }

    @Test
    fun `journalpost allerede knyttet til rett saksnummer i K9Sak`() {
        mockInfotrygd(pleietrengende = true)
        mockK9Sak(søker = true)
        mockHentSaksnummer()
        mockHentJournalpost(fagsaksystem = "K9", fagsakId = "$saksnummer")
        mockForsikreSakskobling()

        val (httpStatus, k9Saksnummer) = request()
        assertEquals(HttpStatusCode.OK, httpStatus)
        assertEquals(saksnummer, k9Saksnummer)
    }

    @Test
    fun `journalpost allerede knyttet til annet fagsystem`() {
        mockInfotrygd(pleietrengende = true)
        mockK9Sak(søker = true)
        mockHentSaksnummer()
        mockHentJournalpost(fagsaksystem = "FOO", fagsakId = "BAR")

        val (httpStatus, errorType) = request()
        assertEquals(HttpStatusCode.Conflict, httpStatus)
        assertEquals(URI("punsjbolle://ikke-støttet-journalpost"), errorType)
    }


    @Test
    fun `Ingen authorization header`() {
        val (httpStatus, k9Saksnummer) = request(jwt = null)
        assertEquals(HttpStatusCode.Unauthorized, httpStatus)
        assertNull(k9Saksnummer)
    }

    @Test
    fun `Feil audience`() {
        val (httpStatus, k9Saksnummer) = request(jwt = Azure.V2_0.generateJwt(
            clientId = "foo",
            audience = "k9-sak"
        ))
        assertEquals(HttpStatusCode.Forbidden, httpStatus)
        assertNull(k9Saksnummer)
    }

    @Test
    fun `Feil issuer`() {
        val (httpStatus, k9Saksnummer) = request(jwt = Azure.V1_0.generateJwt(
            clientId = "foo",
            audience = "k9-punsjbolle"
        ))
        assertEquals(HttpStatusCode.Unauthorized, httpStatus)
        assertNull(k9Saksnummer)
    }

    private fun request(
        periode: Periode? = null,
        jwt: String? = Azure.V2_0.generateJwt(
            clientId = "foo",
            audience = "k9-punsjbolle"
        )) = withTestApplication( { punsjbolle(applicationContext)}) {
        handleRequest(HttpMethod.Post, "/api/saksnummer") {
            addHeader(HttpHeaders.XCorrelationId, "${UUID.randomUUID()}")
            addHeader(HttpHeaders.ContentType, "application/json")
            jwt?.let { addHeader(HttpHeaders.Authorization, "Bearer $it") }
            setBody(requestBody(periode = periode))
        }.let {
            val status = it.response.status()!!
            val saksnummerEllerErrorType : Any? = when (status) {
                HttpStatusCode.OK -> JSONObject(it.response.content!!).getString("saksnummer").somK9Saksnummer()
                HttpStatusCode.Conflict -> URI(JSONObject(it.response.content!!).getString("type"))
                else -> null
            }
            status to saksnummerEllerErrorType
        }
    }


    private fun assertInfotrygdKalt(forventet:Boolean) = when (forventet) {
        true -> coVerify(exactly = 1) { infotrygdClientMock.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any()) }
        false -> coVerify { infotrygdClientMock.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any()) wasNot Called }
    }

    private fun assertK9SakKalt(forventet:Boolean) = when (forventet) {
        true -> coVerify(exactly = 1) { k9SakClientMock.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any(), any()) }
        false -> coVerify { k9SakClientMock.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any(), any()) wasNot Called }
    }

    private fun mockInfotrygd(søker: Boolean = false, pleietrengende: Boolean = false, annenPart: Boolean = false)  = coEvery {
        infotrygdClientMock.harLøpendeSakSomInvolvererEnAv(any(),any(), any(), any(), any()) }.returns(
            RutingGrunnlag(søker = søker, pleietrengende = pleietrengende, annenPart = annenPart)
        )

    private fun mockK9Sak(søker: Boolean = false, pleietrengende: Boolean = false, annenPart: Boolean = false) {
        coEvery {
            k9SakClientMock.harLøpendeSakSomInvolvererEnAv(any(),any(), any(), any(), any(), any()) }.returns(
            RutingGrunnlag(søker = søker, pleietrengende = pleietrengende, annenPart = annenPart)
        )
        coEvery { k9SakClientMock.inngårIUnntaksliste(any(), any(), any()) }.returns(false)
    }

    private fun mockHentSaksnummer() = coEvery { k9SakClientMock.hentSaksnummer(any(),any()) }.returns(saksnummer)

    private fun mockForsikreSakskobling() = coEvery { sakClientMock.forsikreSakskoblingFinnes(any(),any(),any()) }.returns(Unit)

    private fun mockHentJournalpost(fagsaksystem: String? = null, fagsakId: String? = null) {
        val sakskobling = fagsaksystem != null && fagsakId != null
        val sak =  when (sakskobling) { false -> null else -> Journalpost.Sak(fagsaksystem = fagsaksystem!!, fagsakId = fagsakId!!)}
        val journalpoststatus = when (sakskobling) {false -> "MOTTATT" else -> "JOURNALFØRT"}
        coEvery { safClientMock.hentJournalpost(any(), any()) }.returns(Journalpost(
            journalpostId = journalpostId,
            journalposttype = "I",
            journalpoststatus = journalpoststatus,
            opprettet = opprettet.atStartOfDay(),
            eksternReferanse = null,
            brevkode = null,
            sak = sak
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
        private fun requestBody(periode: Periode?) = """
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
              "journalpostId": "$journalpostId",
              "periode": ${periode?.let { """"$it"""" }}
            }
        """.trimIndent()
    }
}