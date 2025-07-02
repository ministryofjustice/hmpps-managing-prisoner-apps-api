package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.ManageUsersApiExtension.Companion.manageUsersApi
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentRepository
import java.time.Duration

class EstablishmentIntegrationTest(
  @Autowired private val establishmentRepository: EstablishmentRepository,
) : IntegrationTestBase() {

  private val establishmentIdFirst = "TEST_ESTABLISHMENT_FIRST"
  private val establishmentIdSecond = "TEST_ESTABLISHMENT_SECOND"
  private val establishmentIdThird = "TEST_ESTABLISHMENT_THIRD"

  @BeforeEach
  fun setup() {
    populateEstablishments()

    manageUsersApi.start()
    manageUsersApi.stubStaffDetailsFound(loggedUserId)

    webTestClient = webTestClient
      .mutate()
      .responseTimeout(Duration.ofMillis(30000))
      .build()
  }

  @AfterEach()
  fun teardown() {
    establishmentRepository.deleteAll()
  }

  @Test
  fun `get list of establishments`() {
    val response = webTestClient.get()
      .uri("/v1/establishments")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<Set<String>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as Set<String>
    assertEquals(3, response.size)
  }

  @Test
  fun `get apptypes by establishments`() {
    val response = webTestClient.get()
      .uri("/v1/establishments/apps/types")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISON")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<List<String>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as List<String>
    assertEquals(AppType.entries.size, response.size)
  }

  private fun populateEstablishments() {
    establishmentRepository.save(Establishment(establishmentIdFirst, "ESTABLISHMENT_NAME_1"))
    establishmentRepository.save(Establishment(establishmentIdSecond, "ESTABLISHMENT_NAME_2"))
    establishmentRepository.save(Establishment(establishmentIdThird, "ESTABLISHMENT_NAME_3"))
  }
}
