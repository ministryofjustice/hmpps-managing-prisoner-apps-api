package uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.ExampleApiExtension
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.ExampleApiExtension.Companion.exampleApi
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.PrisonerSearchApiExtension.Companion.prisonerSearchApi
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@ExtendWith(HmppsAuthApiExtension::class, ExampleApiExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {
  protected val objectMapper: ObjectMapper = ObjectMapper()
    .registerModules(JavaTimeModule(), kotlinModule())
    .setSerializationInclusion(JsonInclude.Include.NON_NULL).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  internal fun setAuthorisation(
    username: String? = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  protected fun stubPingWithResponse(status: Int) {
    hmppsAuth.stubHealthPing(status)
    exampleApi.stubHealthPing(status)
    prisonerSearchApi.stubHealthPing(status)
  }
}
