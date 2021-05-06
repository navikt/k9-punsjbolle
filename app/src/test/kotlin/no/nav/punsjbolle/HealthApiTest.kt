package no.nav.punsjbolle

import  io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.testing.*
import no.nav.punsjbolle.testutils.ApplicationContextExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class HealthApiTest(
    applicationContextBuilder: ApplicationContext.Builder) {
    private val applicationContext = applicationContextBuilder.build()

    @Test
    fun `Test health end point`() {
        withTestApplication({ punsjbolle(applicationContext) }) {
            handleRequest(HttpMethod.Get, "/health").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())
            }
        }
    }
}