package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppRequestPrisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppResponsePrisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.ManageUsersApiExtension.Companion.manageUsersApi
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.PrisonerSearchApiExtension.Companion.prisonerSearchApi
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppFileRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationGroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationTypeRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator.Companion.CONTACT_NUMBER
import java.time.Duration

class AppResourcePrisonerFacingIntegrationTest(
  @Autowired private val appRepository: AppRepository,
  @Autowired private val groupRepository: GroupRepository,
  @Autowired private val establishmentRepository: EstablishmentRepository,
  @Autowired private val commentRepository: CommentRepository,
  @Autowired private val applicationGroupRepository: ApplicationGroupRepository,
  @Autowired private val applicationTypeRepository: ApplicationTypeRepository,
  @Autowired private val appFileRepository: AppFileRepository,
) : AppResourceIntegrationTest(
  appRepository,
  groupRepository,
  establishmentRepository,
  commentRepository,
  applicationGroupRepository,
  applicationTypeRepository,
  appFileRepository,
)  {

  @BeforeEach
  override fun setUp() {
    appRepository.deleteAll()
    groupRepository.deleteAll()
    establishmentRepository.deleteAll()
    applicationGroupRepository.deleteAll()
    applicationTypeRepository.deleteAll()
    appFileRepository.deleteAll()

    populateEstablishments()
    populateGroups()
    populateApps()
    populateApplicationGroupsAndTypes()

    prisonerSearchApi.start()
    prisonerSearchApi.stubPrisonerSearchFound(loggedUserId)

    // manageUsersApi.start()
    // manageUsersApi.stubStaffDetailsFound(loggedUserId)

    webTestClient = webTestClient
      .mutate()
      .responseTimeout(Duration.ofMillis(30000))
      .build()
  }

  @AfterEach
  override fun tearOff() {
    appRepository.deleteAll()
    groupRepository.deleteAll()
    establishmentRepository.deleteAll()
  }

  @Test
  fun `submit an app by prisoner`() {
    var response = webTestClient.post()
      .uri("/v1/prisoners/apps")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(
        AppRequestPrisoner(
          null,
          1,
          true,
          listOf(
            HashMap<String, Any>()
              .apply {
                // put("amount", 10)
                put("contact-number", CONTACT_NUMBER)
                // put("firstName", "John")
                // put("lastName", "Smith")
              },
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<AppResponseDto<Any, Any>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as AppResponsePrisoner<Any, Any>

    Assertions.assertEquals(applicationTypeOne, response.applicationType.id)
    Assertions.assertEquals(requestedByFirst, response.requestedBy)
    Assertions.assertEquals(AppStatus.PENDING, response.status)
    Assertions.assertEquals(1, response.requests.size)
    Assertions.assertNotNull(response.requests.get(0)["id"])
    Assertions.assertEquals(response.requests.get(0)["contact-number"], CONTACT_NUMBER)

  }
}
