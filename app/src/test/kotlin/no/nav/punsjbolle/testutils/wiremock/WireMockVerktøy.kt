package no.nav.punsjbolle.testutils.wiremock

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern

internal object WireMockVerktøy {
    internal fun MappingBuilder.withAuthorizationHeader() =
        withHeader("Authorization", WireMock.containing("Bearer e"))

    internal fun MappingBuilder.withDefaultGetHeaders() =
        withAuthorizationHeader()
        .withHeader("Accept", WireMock.equalTo("application/json"))
        .withHeader("Nav-Consumer-Id", WireMock.equalTo("k9-punsjbolle"))
        .withHeader("Nav-Callid", AnythingPattern())

    internal fun MappingBuilder.withDefaultPostHeaders() =
        withDefaultGetHeaders()
        .withHeader("Content-Type", WireMock.equalTo("application/json"))

    internal fun ResponseDefinitionBuilder.withJson(json: String) =
        withHeader("Content-Type", "application/json")
        .withStatus(200)
        .withBody(json)
}