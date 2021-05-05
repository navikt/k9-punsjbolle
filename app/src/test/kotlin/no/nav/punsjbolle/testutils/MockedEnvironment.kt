package no.nav.punsjbolle.testutils

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.punsjbolle.testutils.wiremock.mockK9Sak
import no.nav.punsjbolle.testutils.wiremock.mockSaf
import java.io.File
import java.nio.file.Files

internal class MockedEnvironment {
    private fun embeddedPostgress(tempDir: File) = EmbeddedPostgres.builder()
        .setOverrideWorkingDirectory(tempDir)
        .setDataDirectory(tempDir.resolve("datadir"))
        .start()

    internal val embeddedPostgres = embeddedPostgress(Files.createTempDirectory("tmp_postgres").toFile())

    internal val wireMockServer = WireMockBuilder()
        .withAzureSupport()
        .withNaisStsSupport()
        .build()
        .mockK9Sak()
        .mockSaf()

    internal fun start() = this
    internal fun stop() {
        embeddedPostgres.close()
        wireMockServer.stop()
    }
}