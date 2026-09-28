package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.EstablishmentApplicationTypeRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentApplicationTypeResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.ManageUsersApiExtension.Companion.manageUsersApi
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationGroup
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EstablishmentApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationGroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationTypeRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentApplicationTypeRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentRepository
import java.time.Duration
import java.util.UUID

class AdminResourceIntegrationTest(
  @Autowired private val establishmentRepository: EstablishmentRepository,
  @Autowired private val applicationGroupRepository: ApplicationGroupRepository,
  @Autowired private val applicationTypeRepository: ApplicationTypeRepository,
  @Autowired private val establishmentApplicationTypeRepository: EstablishmentApplicationTypeRepository,
) : IntegrationTestBase() {

  private val establishmentId = "TEST_ESTABLISHMENT_FIRST"
  private lateinit var mappingOne: EstablishmentApplicationType
  private lateinit var mappingTwo: EstablishmentApplicationType

  @BeforeEach
  fun setup() {
    establishmentApplicationTypeRepository.deleteAll()
    applicationTypeRepository.deleteAll()
    applicationGroupRepository.deleteAll()
    establishmentRepository.deleteAll()

    populateData()

    manageUsersApi.start()
    manageUsersApi.stubStaffDetailsFound(loggedUserId)

    webTestClient = webTestClient
      .mutate()
      .responseTimeout(Duration.ofMillis(30000))
      .build()
  }

  @AfterEach
  fun tearDown() {
    establishmentApplicationTypeRepository.deleteAll()
    applicationTypeRepository.deleteAll()
    applicationGroupRepository.deleteAll()
    establishmentRepository.deleteAll()
  }

  @Test
  fun `get all department mappings for logged staff establishment`() {
    val response = webTestClient.get()
      .uri("/v1/admin/department-mappings")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<List<EstablishmentApplicationTypeResponseDto>>() {})
      .returnResult()
      .responseBody as List<EstablishmentApplicationTypeResponseDto>

    assertEquals(2, response.size)
    assertEquals(listOf(mappingTwo.id, mappingOne.id), response.map { it.id })
    assertEquals(listOf(false, true), response.map { it.active })
  }

  @Test
  fun `patch updates department mappings for logged staff establishment`() {
    val newDepartment = UUID.randomUUID()
    val request = listOf(
      EstablishmentApplicationTypeRequestDto(
        id = mappingOne.id!!,
        applicationTypeId = mappingOne.applicationType.id,
        establishmentId = establishmentId,
        departmentId = newDepartment,
        active = false,
      ),
      EstablishmentApplicationTypeRequestDto(
        id = mappingTwo.id!!,
        applicationTypeId = mappingTwo.applicationType.id,
        establishmentId = establishmentId,
        departmentId = null,
        active = true,
      ),
    )

    val response = webTestClient.patch()
      .uri("/v1/admin/department-mappings")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<List<EstablishmentApplicationTypeResponseDto>>() {})
      .returnResult()
      .responseBody as List<EstablishmentApplicationTypeResponseDto>

    assertEquals(2, response.size)
    assertEquals(false, response.first { it.id == mappingOne.id }.active)
    assertEquals(newDepartment, response.first { it.id == mappingOne.id }.departmentId)
    assertEquals(true, response.first { it.id == mappingTwo.id }.active)

    val updatedOne = establishmentApplicationTypeRepository.findById(mappingOne.id!!).get()
    val updatedTwo = establishmentApplicationTypeRepository.findById(mappingTwo.id!!).get()
    assertEquals(false, updatedOne.active)
    assertEquals(newDepartment, updatedOne.departmentId)
    assertEquals(true, updatedTwo.active)
    assertNotNull(updatedOne.lastModifiedDate)
  }

  private fun populateData() {
    val establishment = establishmentRepository.save(
      Establishment(
        establishmentId,
        "Test Establishment",
        false,
        emptySet(),
        emptySet(),
      ),
    )
    val appGroup = applicationGroupRepository.save(
      ApplicationGroup(1L, "PIN apps"),
    )

    val appTypeOne = applicationTypeRepository.save(
      ApplicationType(1L, "Add social contact", false, false, false, appGroup),
    )
    val appTypeTwo = applicationTypeRepository.save(
      ApplicationType(2L, "Add official contact", false, false, false, appGroup),
    )

    mappingOne = establishmentApplicationTypeRepository.save(
      EstablishmentApplicationType(
        establishment = establishment,
        applicationType = appTypeOne,
        active = true,
      ),
    )
    mappingTwo = establishmentApplicationTypeRepository.save(
      EstablishmentApplicationType(
        establishment = establishment,
        applicationType = appTypeTwo,
        active = false,
      ),
    )
  }
}
