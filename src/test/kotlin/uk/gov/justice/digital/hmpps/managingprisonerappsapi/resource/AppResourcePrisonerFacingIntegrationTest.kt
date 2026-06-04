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
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppRequestPrisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.CommentRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppResponsePrisonerDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.ApplicationGroupResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.CommentResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PageResultComments
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrisonerApplicationTypeCount
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrisonerAppsPage
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrisonerDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.PrisonerSearchApiExtension.Companion.prisonerSearchApi
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationGroup
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.CommentVisibility
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppFileRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationGroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationTypeRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator.Companion.CONTACT_NUMBER
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

class AppResourcePrisonerFacingIntegrationTest(
  @Autowired private val appRepository: AppRepository,
  @Autowired private val groupRepository: GroupRepository,
  @Autowired private val commentRepository: CommentRepository,
  @Autowired private val establishmentRepository: EstablishmentRepository,
  @Autowired private val applicationGroupRepository: ApplicationGroupRepository,
  @Autowired private val applicationTypeRepository: ApplicationTypeRepository,
  @Autowired private val appFileRepository: AppFileRepository,
) : IntegrationTestBase() {

  private val establishmentIdFirst = "TEST_ESTABLISHMENT_FIRST"
  private val establishmentIdSecond = "TEST_ESTABLISHMENT_SECOND"
  private val establishmentIdThird = "TEST_ESTABLISHMENT_THIRD"
  private val assignedGroupFirst = Generators.timeBasedEpochGenerator().generate()
  private val assignedGroupFirstName = "Business Hub"
  private val assignedGroupSecond = Generators.timeBasedEpochGenerator().generate()
  private val assignedGroupSecondName = "OMU"
  private val requestedByFirst = "A12345"
  private val requestedByFirstMainName = "John"
  private val requestedByFirstSurname = "Smith"

  private val applicationGroupOne = 1L
  private val applicationTypeOne = 1L
  private val applicationTypeTwo = 2L
  private val applicationTypeThree = 3L
  private val applicationTypeFour = 4L

  private val applicationGroupOneName = "Bt PIN PHONES"
  private val applicationTypeOneName = "Add new Social Contact"
  private val applicationTypeTwoName = "Add new Official Contact"
  private val applicationTypeThreeName = "Remove Contact"
  private val applicationTypeFourName = "Add Generic Pin Phone enquiry"

  private lateinit var app: App

  @BeforeEach
  fun setUp() {
    loggedUserId = requestedByFirst
    appRepository.deleteAll()
    groupRepository.deleteAll()
    establishmentRepository.deleteAll()
    applicationGroupRepository.deleteAll()
    applicationTypeRepository.deleteAll()
    appFileRepository.deleteAll()
    commentRepository.deleteAll()

    populateEstablishments()
    populateGroups()
    populateApps()
    populateApplicationGroupsAndTypes()

    prisonerSearchApi.start()
    prisonerSearchApi.stubPrisonerSearchFound(loggedUserId)

    webTestClient = webTestClient
      .mutate()
      .responseTimeout(Duration.ofMillis(30000))
      .build()
  }

  @AfterEach
  fun tearOff() {
    appRepository.deleteAll()
    groupRepository.deleteAll()
    establishmentRepository.deleteAll()
    commentRepository.deleteAll()
  }

  @Test
  fun `submit an app by prisoner, get app by id, get list of apps`() {
    appRepository.deleteAll()
    // saved an app
    var appResponse = webTestClient.post()
      .uri("/v1/prisoners/apps")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_FACING_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(
        AppRequestPrisoner(
          null,
          1L,
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
      .expectBody(object : ParameterizedTypeReference<AppResponsePrisonerDto<Any, Prisoner>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as AppResponsePrisonerDto<Any, Prisoner>

    Assertions.assertEquals(applicationTypeOne, appResponse.applicationType.id)
    Assertions.assertEquals(loggedUserId, appResponse.requestedBy.userId)
    Assertions.assertEquals(AppStatus.PENDING, appResponse.status)
    Assertions.assertEquals(1, appResponse.requests.size)
    Assertions.assertNotNull(appResponse.requests.get(0)["id"])
    Assertions.assertEquals(appResponse.requests.get(0)["contact-number"], CONTACT_NUMBER)

    // Get app by id
    appResponse = webTestClient.get()
      .uri("/v1/prisoners/apps/${appResponse.id}")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_FACING_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<AppResponsePrisonerDto<Any, Prisoner>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as AppResponsePrisonerDto<Any, Prisoner>

    Assertions.assertNotNull(appResponse)

    // get apps
    val appsResponse = webTestClient.get()
      .uri("/v1/prisoners/apps?pageNum=1")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_FACING_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<PrisonerAppsPage>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as PrisonerAppsPage
    Assertions.assertEquals(1, appsResponse.page)
    Assertions.assertEquals(1, appsResponse.totalRecords)
    Assertions.assertNotNull(appsResponse.apps[0])
  }

  @Test
  fun `get app groups and types for logged prisoner`() {
    val appsResponse = webTestClient.get()
      .uri("/v1/prisoners/apps/groups")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_FACING_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<List<ApplicationGroupResponse>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as List<ApplicationGroupResponse>
    Assertions.assertNotNull(appsResponse.get(0))
  }

  @Test
  fun `get logged prisoner apps count by application type`() {
    appRepository.deleteAll()
    var response = webTestClient.get()
      .uri("/v1/prisoners/apps/$applicationTypeOne/pending")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_FACING_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<PrisonerApplicationTypeCount>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as PrisonerApplicationTypeCount
    Assertions.assertEquals(0, response.totalAppsInPending)

    // add app request
    val app = webTestClient.post()
      .uri("/v1/prisoners/apps")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_FACING_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(
        AppRequestPrisoner(
          null,
          1L,
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
      .expectBody(object : ParameterizedTypeReference<AppResponsePrisonerDto<Any, Prisoner>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as AppResponsePrisonerDto<Any, Prisoner>

    Assertions.assertNotNull(app)

    // verify count is 1 in getting app count by application type
    response = webTestClient.get()
      .uri("/v1/prisoners/apps/$applicationTypeOne/pending")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_FACING_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<PrisonerApplicationTypeCount>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as PrisonerApplicationTypeCount
    Assertions.assertEquals(1, response.totalAppsInPending)
  }

  @Test
  fun `add a comment for the app`() {
    val message = "Do you need more information"
    val body = CommentRequestDto(message, CommentVisibility.STAFF_AND_PRISONER)
    val response = webTestClient.post()
      .uri("/v1/prisoners/apps/${app.id}/comments")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_FACING_APPS")))
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
  }

  @Test
  fun `get comment by id`() {
    val message = "This needs to be checked again"
    val body = CommentRequestDto(message, CommentVisibility.STAFF_AND_PRISONER)
    val response = webTestClient.post()
      .uri("/v1/prisoners/apps/${app.id}/comments")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_FACING_APPS")))
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
      .uri("/v1/prisoners/apps/${app.id}/comments/${response.id}")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_FACING_APPS")))
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
      .uri("/v1/prisoners/apps/${app.id}/comments/${response.id}?createdBy=true")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_FACING_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      // .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectBody(object : ParameterizedTypeReference<CommentResponseDto<PrisonerDto>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as CommentResponseDto<PrisonerDto>

    Assertions.assertNotNull(message, resp.message)
    Assertions.assertEquals(app.id, resp.appId)
    Assertions.assertEquals(app.requestedBy, resp.prisonerNumber)
  }

  @Test
  fun `get comments by app id`() {
    val message = "Do you need more information?"
    val body = CommentRequestDto(
      message,
      CommentVisibility.STAFF_AND_PRISONER,
    )
    webTestClient.post()
      .uri("/v1/prisoners/apps/${app.id}/comments")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_FACING_APPS")))
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
      .uri("/v1/prisoners/apps/${app.id}/comments?page=1&size=10")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_FACING_APPS")))
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

  protected fun populateEstablishments() {
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

  protected fun populateGroups() {
    groupRepository.save(
      DataGenerator.generateGroups(
        assignedGroupFirst,
        establishmentIdFirst,
        assignedGroupFirstName,
        listOf(1L, 3L),
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
        listOf(1L, 2L),
        GroupType.WING,
      ),
    )
  }

  protected fun populateApplicationGroupsAndTypes() {
    var applicationGroupOne = ApplicationGroup(applicationGroupOne, applicationGroupOneName)

    applicationGroupOne = applicationGroupRepository.save<ApplicationGroup>(applicationGroupOne)
    val addSocialContact =
      ApplicationType(applicationTypeOne, applicationTypeOneName, false, false, false, applicationGroupOne)
    val removeContact =
      ApplicationType(applicationTypeTwo, applicationTypeTwoName, false, false, false, applicationGroupOne)
    val addOfficialContact =
      ApplicationType(applicationTypeThree, applicationTypeThreeName, false, false, false, applicationGroupOne)
    val addGenericPinPhoneEnquiry =
      ApplicationType(applicationTypeFour, applicationTypeFourName, true, false, true, applicationGroupOne)
    applicationTypeRepository.saveAll<ApplicationType>(
      listOf<ApplicationType>(
        addSocialContact,
        removeContact,
        addOfficialContact,
        addGenericPinPhoneEnquiry,
      ),
    )
    /*// val applicationGroupOne = ApplicationGroup(applicationGroupOne, applicationGroupOneName, listOf(addSocialContact, removeContact, addOfficialContact, addGenericPinPhoneEnquiry))

    // applicationGroupRepository.save<ApplicationGroup>(applicationGroupOne)
    // addOfficialContact.applicationGroup = applicationGroupOne
    // applicationTypeRepository.save(addOfficialContact)
    val appgrp = applicationTypeRepository.findById(applicationTypeOne)
    // println(appgrp)*/
  }

  protected fun populateApps() {
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
        AppStatus.PENDING,
        assignedGroupFirst,
        false,
      ),
    )
  }
}
