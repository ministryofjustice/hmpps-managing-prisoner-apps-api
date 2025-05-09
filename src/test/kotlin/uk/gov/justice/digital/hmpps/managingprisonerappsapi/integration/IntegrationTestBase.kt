package uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration

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
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.ManageUsersApiExtension.Companion.manageUsersApi
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.PrisonerSearchApiExtension.Companion.prisonerSearchApi
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@ExtendWith(HmppsAuthApiExtension::class, ExampleApiExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {
  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  protected val loggedUserId = "AUTH_ADM"

  internal fun setAuthorisation(
    username: String? = loggedUserId,
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  protected fun stubPingWithResponse(status: Int) {
    hmppsAuth.stubHealthPing(status)
    exampleApi.stubHealthPing(status)
    prisonerSearchApi.stubHealthPing(status)
    manageUsersApi.stubHealthPing(status)
  }
}
