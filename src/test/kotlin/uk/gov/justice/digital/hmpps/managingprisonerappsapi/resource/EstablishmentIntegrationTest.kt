package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppTypeResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.ApplicationGroupResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.ManageUsersApiExtension.Companion.manageUsersApi
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationGroup
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationGroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationTypeRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentRepository
import java.time.Duration

class EstablishmentIntegrationTest(
  @Autowired private val establishmentRepository: EstablishmentRepository,
  @Autowired private val applicationGroupRepository: ApplicationGroupRepository,
  @Autowired private val applicationTypeRepository: ApplicationTypeRepository,
) : IntegrationTestBase() {

  private val establishmentIdFirst = "TEST_ESTABLISHMENT_FIRST"
  private val establishmentIdSecond = "TEST_ESTABLISHMENT_SECOND"
  private val establishmentIdThird = "TEST_ESTABLISHMENT_THIRD"

  @BeforeEach
  fun setup() {
    populateEstablishments()
    populateApplicationGroupsAndTypes()

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
  fun `get app types by establishments`() {
    var response = webTestClient.get()
      .uri("/v1/establishments/apps/types")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISON")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<List<AppTypeResponse>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as List<AppTypeResponse>
    assertEquals(1, response.size)
    assertEquals(AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT.toString(), response.get(0).key)

    // set all apptypes
    establishmentRepository.save(
      Establishment(
        establishmentIdFirst,
        "ESTABLISHMENT_NAME_1",
        AppType.entries.toSet(),
        false,
        setOf(),
        setOf(),
      ),
    )

    response = webTestClient.get()
      .uri("/v1/establishments/apps/types")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISON")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<List<AppTypeResponse>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as List<AppTypeResponse>
    assertEquals(AppType.entries.size, response.size)
  }

  @Test
  fun `get app groups and types by establishments`() {
    var response = webTestClient.get()
      .uri("/v2/establishments/apps/groups")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISON")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<List<ApplicationGroupResponse>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as List<ApplicationGroupResponse>
    assertEquals(2, response.size)
  }

  private fun populateApplicationGroupsAndTypes() {
    val at1 = ApplicationType(1, "Add new social PIN phone contact", false, false, false)

    val at2 = ApplicationType(2, "Add new official PIN phone contact", false, false, false)
    val at3 = ApplicationType(3, "Remove PIN phone contact", false, false, false)
    val at4 = ApplicationType(4, "Add generic contact request", true, false, true)

    val at5 = ApplicationType(5, "Add emergency PIN phone credit", false, false, false)
    val at6 = ApplicationType(6, "Swap visiting orders (VOs) for PIN credit", false, false, false)
    val at7 = ApplicationType(7, "Generic credit and Visit", true, false, false)
    applicationTypeRepository.saveAll<ApplicationType>(listOf(at1, at2, at3, at4, at5, at6, at7, at7))
    val applicationGroup1 = ApplicationGroup(1, "Pin Phone Contact Apps", listOf<ApplicationType>(at1, at2, at3, at4))
    val applicationGroup2 = ApplicationGroup(2, "Emergency Credit and Vist", listOf(at5, at6, at7))
    at1.applicationGroup = applicationGroup1
    at2.applicationGroup = applicationGroup1
    at3.applicationGroup = applicationGroup1
    at4.applicationGroup = applicationGroup1
    at5.applicationGroup = applicationGroup1
    at6.applicationGroup = applicationGroup2
    at7.applicationGroup = applicationGroup2
    applicationGroupRepository.saveAll<ApplicationGroup>(listOf(applicationGroup1, applicationGroup2))
    applicationTypeRepository.saveAll(listOf(at1, at2, at3, at4, at5, at6, at7, at7))
  }

  private fun populateEstablishments() {
    establishmentRepository.save(
      Establishment(
        establishmentIdFirst,
        "ESTABLISHMENT_NAME_1",
        setOf(AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT),
        false,
        setOf(),
        setOf(),
      ),
    )
    establishmentRepository.save(
      Establishment(
        establishmentIdSecond,
        "ESTABLISHMENT_NAME_2",
        AppType.entries.toSet(),
        false,
        setOf(),
        setOf(),
      ),
    )
    establishmentRepository.save(
      Establishment(
        establishmentIdThird,
        "ESTABLISHMENT_NAME_3",
        AppType.entries.toSet(),
        false,
        setOf(),
        setOf(),
      ),
    )
  }
}
