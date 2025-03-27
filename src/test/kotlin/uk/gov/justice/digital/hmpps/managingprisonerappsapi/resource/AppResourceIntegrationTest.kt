package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.PrisonerSearchApiExtension.Companion.prisonerSearchApi
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator
import java.time.Duration

class AppResourceIntegrationTest(@Autowired private var appRepository: AppRepository) : IntegrationTestBase() {
  @LocalServerPort
  private val port = 0

  private val baseUrl = "http://localhost"

  private val stubPrisoner = Prisoner("prisonerId", "123", "PrisonerfirstName", "PrisonerlastName", UserCategory.PRISONER, "PrisonerlocationDescription", "iep")

  @BeforeEach
  fun setUp() {
    hmppsAuth.stubGrantToken()
    prisonerSearchApi.start()
    prisonerSearchApi.stubPrisonerSearchFound(objectMapper)
    webTestClient = webTestClient
      .mutate()
      .responseTimeout(Duration.ofMillis(30000))
      .build()
  }

  @AfterEach
  fun tearOff() {
    appRepository.deleteAll()
  }

  @Test
  fun `submit an app`() {
    webTestClient.post()
      .uri("/v1/prisoners/G12345/apps")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(DataGenerator.generateAppRequestDto())
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
  }

  @Test
  fun `submit an app with no roles`() {
    webTestClient.post()
      .uri("/v1/prisoners/G12345/apps")
      .headers(setAuthorisation())
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(DataGenerator.generateAppRequestDto())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `get an app by id`() {
    val app = DataGenerator.generateApp()
    appRepository.save(app)

    webTestClient.get()
      .uri("/v1/prisoners/G12345/apps/${app.id}")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectBody(AppResponseDto::class.java)
      .returnResult()
  }

  @Test
  fun `get an app by id with prisoner`() {
    val app = DataGenerator.generateApp()
    appRepository.save(app)

    val responseBody = webTestClient.get()
      .uri("/v1/prisoners/G12345/apps/${app.id}?requestedBy=true")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectBody(AppResponseDto::class.java)
      .returnResult()
      .responseBody as AppResponseDto
    //  val requestedBy = responseBody.requestedBy as RequestedByDto
    //    assertThat(requestedBy.firstName).isNotNull
    // println(requestedBy)
  }
}
