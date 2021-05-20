package no.nav.punsjbolle.testutils

import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.punsjbolle.testutils.wiremock.mockInfotrygdGrunnlagPaaroerendeSykdom
import no.nav.punsjbolle.testutils.wiremock.mockK9Sak
import no.nav.punsjbolle.testutils.wiremock.mockSaf
import no.nav.punsjbolle.testutils.wiremock.mockSak

internal class MockedEnvironment {
    internal val wireMockServer = WireMockBuilder()
        .withAzureSupport()
        .withNaisStsSupport()
        .build()
        .mockK9Sak()
        .mockSak()
        .mockSaf()
        .mockInfotrygdGrunnlagPaaroerendeSykdom()

    internal fun start() = this
    internal fun stop() {
        wireMockServer.stop()
    }
}