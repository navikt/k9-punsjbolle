package no.nav.punsjbolle.testutils

import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.punsjbolle.testutils.wiremock.mockK9Sak
import no.nav.punsjbolle.testutils.wiremock.mockSaf

internal class MockedEnvironment {
    internal val wireMockServer = WireMockBuilder()
        .withAzureSupport()
        .withNaisStsSupport()
        .build()
        .mockK9Sak()
        .mockSaf()

    internal fun start() = this
    internal fun stop() {
        wireMockServer.stop()
    }
}