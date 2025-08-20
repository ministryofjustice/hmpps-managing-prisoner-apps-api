package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import com.fasterxml.uuid.Generators
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AssignedGroupDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator
import java.util.*

class GroupResourceIntegrationTest(
  @Autowired private val groupRepository: GroupRepository,
  @Autowired private val establishmentRepository: EstablishmentRepository,
) : IntegrationTestBase() {
  @LocalServerPort
  private val port = 0

  private val baseUrl = "http://localhost"

  val establishmentIdFirst = "TEST_ESTABLISHMENT_FIRST"
  val assignedGroupFirst = Generators.timeBasedEpochGenerator().generate()
  val assignedGroupFirstName = "Business Hub"
  val assignedGroupSecond = Generators.timeBasedEpochGenerator().generate()
  val assignedGroupSecondName = "OMU"

  @BeforeEach
  fun setup() {
    groupRepository.deleteAll()
    establishmentRepository.deleteAll()

    populateEstablishments()
    populateGroups()
  }

  @AfterEach
  fun teardown() {
    groupRepository.deleteAll()
    establishmentRepository.deleteAll()
  }

  @Test
  fun `get groups by establishment id`() {
    val response = webTestClient.get()
      .uri("/v1/groups")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<List<AssignedGroupDto>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as List<AssignedGroupDto>

    Assertions.assertEquals(2, response.size)
    Assertions.assertEquals(assignedGroupFirst, response.get(0).id)
    Assertions.assertEquals(assignedGroupFirstName, response.get(0).name)
    Assertions.assertEquals(establishmentIdFirst, response.get(0).establishment.name)
    Assertions.assertEquals(assignedGroupSecond, response.get(1).id)
    Assertions.assertEquals(assignedGroupSecondName, response.get(1).name)
    Assertions.assertEquals(establishmentIdFirst, response.get(1).establishment.name)
  }

  private fun populateEstablishments() {
    establishmentRepository.save(Establishment(establishmentIdFirst, establishmentIdFirst, AppType.entries.toSet()))
  }

  private fun populateGroups() {
    groupRepository.save(
      DataGenerator.generateGroups(
        assignedGroupFirst,
        establishmentIdFirst,
        assignedGroupFirstName,
        listOf(AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT, AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT),
        GroupType.WING,
      ),
    )
    /*groupRepository.save(
        DataGenerator.generateGroups(
            assignedGroupFirst,
            establishmentIdFirst,
            assignedGroupFirstName,
            listOf(AppType.PIN_PHONE_ADD_NEW_CONTACT, AppType.PIN_PHONE_REMOVE_CONTACT),
            GroupType.WING
        )
    )*/
    groupRepository.save(
      DataGenerator.generateGroups(
        assignedGroupSecond,
        establishmentIdFirst,
        assignedGroupSecondName,
        listOf(AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT, AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT),
        GroupType.WING,
      ),
    )
  }
}
