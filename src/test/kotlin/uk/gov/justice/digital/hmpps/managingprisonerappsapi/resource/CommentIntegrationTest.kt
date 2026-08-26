package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import com.fasterxml.uuid.Generators
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.CommentRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.CommentResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.HistoryResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PageResultComments
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.StaffDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.ManageUsersApiExtension.Companion.manageUsersApi
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.PrisonerSearchApiExtension.Companion.prisonerSearchApi
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationGroup
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.CommentVisibility
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationGroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationTypeRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

class CommentIntegrationTest(
  @Autowired private val appRepository: AppRepository,
  @Autowired private val groupRepository: GroupRepository,
  @Autowired private val establishmentRepository: EstablishmentRepository,
  @Autowired private val commentRepository: CommentRepository,
  @Autowired private val applicationTypeRepository: ApplicationTypeRepository,
  @Autowired private val applicationGroupRepository: ApplicationGroupRepository,
) : IntegrationTestBase() {

  private lateinit var app: App

  companion object {
    val establishmentIdFirst = "TEST_ESTABLISHMENT_FIRST"
    val establishmentIdSecond = "TEST_ESTABLISHMENT_SECOND"
    val establishmentIdThird = "TEST_ESTABLISHMENT_THIRD"
    val assignedGroupFirst = Generators.timeBasedEpochGenerator().generate()
    val assignedGroupFirstName = "Business Hub"
    val assignedGroupSecond = Generators.timeBasedEpochGenerator().generate()
    val assignedGroupSecondName = "OMU"
    val requestedByFirst = "A12345"
    val requestedByFirstMainName = "John"
    val requestedByFirstSurname = "Smith"
    val requestedBySecondMainName = "John"
    val requestedBySecondSurname = "Butler"
    val requestedBySecond = "B12345"
    val requestedByThird = "C12345"
    val requestedByThirdMainName = "Test"
    val requestedByThirdSurname = "User"
    val applicationGroupOne = 1L
    val applicationTypeOne = 1L
    val applicationTypeTwo = 2L
    val applicationTypeThree = 3L
    val applicationTypeFour = 4L

    val applicationGroupOneName = "Bt PIN PHONES"
    val applicationTypeOneName = "Add new Social Contact"
    val applicationTypeTwoName = "Add new Official Contact"
    val applicationTypeThreeName = "Remove Contact"
    val applicationTypeFourName = "Add Generic Pin Phone enquiry"
  }

  @BeforeEach
  fun setup() {
    appRepository.deleteAll()
    groupRepository.deleteAll()
    establishmentRepository.deleteAll()
    commentRepository.deleteAll()
    populateEstablishments()
    populateGroups()
    populateApplicationGroupsAndTypes()
    populateApps()

    prisonerSearchApi.start()
    prisonerSearchApi.stubPrisonerSearchFound(requestedByFirst)

    manageUsersApi.start()
    manageUsersApi.stubStaffDetailsFound(loggedUserId)

    webTestClient = webTestClient
      .mutate()
      .responseTimeout(Duration.ofMillis(60000))
      .build()
  }

  @AfterEach
  fun teardown() {
    appRepository.deleteAll()
    groupRepository.deleteAll()
    establishmentRepository.deleteAll()
    commentRepository.deleteAll()
    applicationTypeRepository.deleteAll()
    applicationGroupRepository.deleteAll()
  }

  @Test
  fun `add a comment for the app`() {
    val message = "This needs to be checked again"
    val body = CommentRequestDto(message, CommentVisibility.STAFF_ONLY)
    val response = webTestClient.post()
      .uri("/v1/prisoners/${app.requestedBy}/apps/${app.id}/comments")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(body)
      .exchange()
      .expectStatus().isCreated
      .expectBody(object : ParameterizedTypeReference<CommentResponseDto<String>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as CommentResponseDto<String>

    Assertions.assertNotNull(message, response.message)
    Assertions.assertEquals(app.id, response.appId)
    Assertions.assertEquals(app.requestedBy, response.prisonerNumber)

    webTestClient.get()
      .uri("/v1/prisoners/$requestedByFirst/apps/${app.id}/history")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<List<HistoryResponse>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as List<HistoryResponse>
  }

  @Test
  fun `get comment by id`() {
    val message = "This needs to be checked again"
    val body = CommentRequestDto(message, CommentVisibility.STAFF_ONLY)
    val response = webTestClient.post()
      .uri("/v1/prisoners/${app.requestedBy}/apps/${app.id}/comments")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(body)
      .exchange()
      .expectStatus().isCreated
      .expectBody(object : ParameterizedTypeReference<CommentResponseDto<String>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as CommentResponseDto<String>
    Assertions.assertNotNull(message, response.message)
    Assertions.assertEquals(app.id, response.appId)
    Assertions.assertEquals(app.requestedBy, response.prisonerNumber)

    var res = webTestClient.get()
      .uri("/v1/prisoners/${app.requestedBy}/apps/${app.id}/comments/${response.id}")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectBody(object : ParameterizedTypeReference<CommentResponseDto<String>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as CommentResponseDto<String>

    Assertions.assertNotNull(message, res.message)
    Assertions.assertEquals(app.id, res.appId)
    Assertions.assertEquals(app.requestedBy, res.prisonerNumber)

    val resp = webTestClient.get()
      .uri("/v1/prisoners/${app.requestedBy}/apps/${app.id}/comments/${response.id}?createdBy=true")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      // .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectBody(object : ParameterizedTypeReference<CommentResponseDto<StaffDto>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as CommentResponseDto<StaffDto>

    Assertions.assertNotNull(message, resp.message)
    Assertions.assertEquals(app.id, resp.appId)
    Assertions.assertEquals(app.requestedBy, resp.prisonerNumber)
  }

  @Test
  fun `get comments by app id`() {
    val message = "This needs to be checked again"
    val body = CommentRequestDto(
      message,
      CommentVisibility.STAFF_ONLY,
    )
    webTestClient.post()
      .uri("/v1/prisoners/${app.requestedBy}/apps/${app.id}/comments")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(body)
      .exchange()
      .expectStatus().isCreated
      .expectBody(object : ParameterizedTypeReference<CommentResponseDto<String>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as CommentResponseDto<String>

    var res = webTestClient.get()
      .uri("/v1/prisoners/${app.requestedBy}/apps/${app.id}/comments?page=1&size=10&visibility=STAFF_ONLY")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectBody(object : ParameterizedTypeReference<PageResultComments>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as PageResultComments

    Assertions.assertEquals(1, res.page)
    Assertions.assertEquals(1, res.totalElements)

    res = webTestClient.get()
      .uri("/v1/prisoners/${app.requestedBy}/apps/${app.id}/comments?page=1&size=10&visibility=STAFF_ONLY")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectBody(object : ParameterizedTypeReference<PageResultComments>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as PageResultComments

    Assertions.assertEquals(1, res.page)
    Assertions.assertEquals(1, res.totalElements)
  }

  private fun populateEstablishments() {
    establishmentRepository.save(
      Establishment(
        establishmentIdFirst,
        "ESTABLISHMENT_NAME_1",
        false,
        setOf(),
        setOf(),
      ),
    )
  }

  private fun populateGroups() {
    groupRepository.save(
      DataGenerator.generateGroups(
        assignedGroupFirst,
        establishmentIdFirst,
        assignedGroupFirstName,
        listOf(1L, 2L),
        GroupType.WING,
      ),
    )
    groupRepository.save(
      DataGenerator.generateGroups(
        assignedGroupSecond,
        establishmentIdFirst,
        assignedGroupSecondName,
        listOf(1L, 2L),
        GroupType.WING,
      ),
    )
  }

  private fun populateApplicationGroupsAndTypes() {
    val addSocialContact = ApplicationType(applicationTypeOne, applicationTypeOneName, false, false, false)
    val addOfficialContact = ApplicationType(applicationTypeTwo, applicationTypeTwoName, false, false, false)
    val removeContact = ApplicationType(applicationTypeThree, applicationTypeThreeName, false, false, false)
    val addGenericPinPhoneEnquiry = ApplicationType(applicationTypeFour, applicationTypeFourName, true, false, true)
    applicationTypeRepository.saveAll(listOf(addSocialContact, addOfficialContact, removeContact, addGenericPinPhoneEnquiry))
    applicationGroupRepository.save(
      ApplicationGroup(
        applicationGroupOne,
        applicationGroupOneName,
        listOf(addSocialContact, addOfficialContact, removeContact, addGenericPinPhoneEnquiry),
      ),
    )
  }

  private fun populateApps() {
    app = appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        null,
        applicationTypeOne,
        applicationGroupOne,
        requestedByFirst,
        LocalDateTime.now(ZoneOffset.UTC).minusDays(4),
        requestedByFirstMainName,
        requestedByFirstSurname,
        AppStatus.NEW,
        assignedGroupFirst,
        false,
      ),
    )
  }
}
