package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppsSearchQueryDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.CommentRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AssignedGroupDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.ManageUsersApiExtension.Companion.manageUsersApi
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.PrisonerSearchApiExtension.Companion.prisonerSearchApi
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator.Companion.CONTACT_NUMBER
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class AppResourceIntegrationTest(
  @Autowired private val appRepository: AppRepository,
  @Autowired private val groupRepository: GroupRepository,
  @Autowired private val establishmentRepository: EstablishmentRepository,
  @Autowired private val commentRepository: CommentRepository,
) : IntegrationTestBase() {

  private val establishmentIdFirst = "TEST_ESTABLISHMENT_FIRST"
  private val establishmentIdSecond = "TEST_ESTABLISHMENT_SECOND"
  private val establishmentIdThird = "TEST_ESTABLISHMENT_THIRD"
  private val assignedGroupFirst = UUID.randomUUID()
  private val assignedGroupFirstName = "Business Hub"
  private val assignedGroupSecond = UUID.randomUUID()
  private val assignedGroupSecondName = "OMU"
  private val requestedByFirst = "A12345"
  private val requestedByFirstMainName = "John"
  private val requestedByFirstSurname = "Smith"
  private val requestedBySecondMainName = "John"
  private val requestedBySecondSurname = "Butler"
  private val requestedBySecond = "B12345"
  private val requestedByThird = "C12345"
  private val requestedByThirdMainName = "Test"
  private val requestedByThirdSurname = "User"

  private lateinit var appIdFirst: UUID
  private lateinit var appIdSecond: UUID

  @BeforeEach
  fun setUp() {
    appRepository.deleteAll()
    groupRepository.deleteAll()
    establishmentRepository.deleteAll()

    populateEstablishments()
    populateGroups()
    populateApps()

    prisonerSearchApi.start()
    prisonerSearchApi.stubPrisonerSearchFound(requestedByFirst)

    manageUsersApi.start()
    manageUsersApi.stubStaffDetailsFound(loggedUserId)

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
  }

  @Test
  fun `submit an app`() {
    var response = webTestClient.post()
      .uri("/v1/prisoners/$requestedByFirst/apps")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(
        DataGenerator.generateAppRequestDto(
          AppType.PIN_PHONE_ADD_NEW_CONTACT,
          LocalDateTime.now(ZoneOffset.UTC),
          requestedByFirstMainName,
          requestedBySecondSurname,
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<AppResponseDto<Any, Any>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as AppResponseDto<Any, Any>

    Assertions.assertEquals(AppType.PIN_PHONE_ADD_NEW_CONTACT, response.appType)
    Assertions.assertEquals(requestedByFirst, response.requestedBy)
    Assertions.assertEquals(AppStatus.PENDING, response.status)
    Assertions.assertEquals(1, response.requests.size)
    Assertions.assertNotNull(response.requests.get(0)["id"])
    Assertions.assertEquals(response.requests.get(0)["contact-number"], CONTACT_NUMBER)

    val newContactNumber = "0987654321"
    val map = response.requests.get(0).toMutableMap()
    map["contact-number"] = newContactNumber
    response = webTestClient.put()
      .uri("/v1/prisoners/$requestedByFirst/apps/${response.id}")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(
        listOf(map),
      )
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<AppResponseDto<Any, Any>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as AppResponseDto<Any, Any>

    Assertions.assertEquals(AppType.PIN_PHONE_ADD_NEW_CONTACT, response.appType)
    Assertions.assertEquals(requestedByFirst, response.requestedBy)
    Assertions.assertEquals(AppStatus.PENDING, response.status)
    Assertions.assertEquals(1, response.requests.size)
    Assertions.assertEquals(newContactNumber, response.requests.get(0)["contact-number"])
  }

  @Test
  fun `search apps by query filters`() {
    val searchQueryDto = AppsSearchQueryDto(
      1,
      10,
      setOf(AppStatus.PENDING),
      setOf(),
      requestedByFirst,
      setOf(),
    )
    webTestClient.post()
      .uri("/v1/prisoners/apps/search")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(searchQueryDto)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody()
      .consumeWith(System.out::println)
  }

  @Test
  fun `search prisoner by text`() {
    webTestClient.get()
      .uri("/v1/prisoners/search?name=jo")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody()
      .consumeWith(System.out::println)

    webTestClient.get()
      .uri("/v1/prisoners/search?name=joh")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody()
      .consumeWith(System.out::println)
  }

  @Test
  fun `forward app request to other group`() {
    // groupRepository.findGroupsByEstablishmentIdAndInitialsAppsIsContaining(establishmentIdFirst)
    val forwardingMessage = "Forwarding  to group $assignedGroupSecond"

    webTestClient.post()
      .uri("/v1/apps/$appIdFirst/forward/groups/$assignedGroupFirst")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isBadRequest

    var response = webTestClient.post()
      .uri("/v1/apps/$appIdFirst/forward/groups/$assignedGroupSecond")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(CommentRequestDto(forwardingMessage))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<AppResponseDto<AssignedGroupDto, String>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as AppResponseDto<AssignedGroupDto, String>

    val comment = commentRepository.findAll().get(0)
    Assertions.assertEquals(forwardingMessage, comment.message)
    Assertions.assertEquals(appIdFirst, comment.appId)
    Assertions.assertEquals(loggedUserId, comment.createdBy)
    Assertions.assertEquals(AppType.PIN_PHONE_ADD_NEW_CONTACT, response.appType)
    Assertions.assertEquals(appIdFirst, response.id)
    Assertions.assertEquals(requestedByFirst, response.requestedBy)
    Assertions.assertEquals(AppStatus.PENDING, response.status)
    Assertions.assertEquals(1, response.requests?.size)
    Assertions.assertEquals(assignedGroupSecond, response.assignedGroup.id)

    // forwarding without forwarding message

    response = webTestClient.post()
      .uri("/v1/apps/$appIdFirst/forward/groups/$assignedGroupFirst")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<AppResponseDto<AssignedGroupDto, String>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as AppResponseDto<AssignedGroupDto, String>

    Assertions.assertEquals(AppType.PIN_PHONE_ADD_NEW_CONTACT, response.appType)
    Assertions.assertEquals(appIdFirst, response.id)
    Assertions.assertEquals(requestedByFirst, response.requestedBy)
    Assertions.assertEquals(AppStatus.PENDING, response.status)
    Assertions.assertEquals(1, response.requests?.size)
    Assertions.assertEquals(assignedGroupFirst, response.assignedGroup.id)
  }

  @Test
  fun `submit an app with no roles`() {
    webTestClient.post()
      .uri("/v1/prisoners/$requestedByFirst/apps")
      .headers(setAuthorisation())
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(
        DataGenerator.generateAppRequestDto(
          AppType.PIN_PHONE_REMOVE_CONTACT,
          LocalDateTime.now(ZoneOffset.UTC),
          requestedByFirstMainName,
          requestedByFirstSurname,
        ),
      )
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `get an app by id`() {
    val app = appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        requestedByFirst,
        LocalDateTime.now(ZoneOffset.UTC),
        requestedByFirstMainName,
        requestedByFirstSurname,
        AppStatus.PENDING,
        groupRepository.findGroupsByEstablishmentIdAndInitialsAppsIsContaining(
          establishmentIdFirst,
          AppType.PIN_PHONE_ADD_NEW_CONTACT,
        ).get(0).id,
      ),
    )
    val response = webTestClient.get()
      .uri("/v1/prisoners/${app.requestedBy}/apps/${app.id}")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectBody(object : ParameterizedTypeReference<AppResponseDto<String, String>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as AppResponseDto<String, String>

    Assertions.assertEquals(app.appType, response.appType)
    Assertions.assertEquals(app.id, response.id)
    Assertions.assertEquals(app.requestedBy, response.requestedBy)
    Assertions.assertEquals(app.status, response.status)
    Assertions.assertEquals(1, response.requests?.size)
    Assertions.assertEquals(app.assignedGroup.toString(), response.assignedGroup.toString())
  }

  @Test
  fun `get an app by id with prisoner`() {
    val app = appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        requestedByFirst,
        LocalDateTime.now(ZoneOffset.UTC),
        requestedByFirstMainName,
        requestedByFirstSurname,
        AppStatus.PENDING,
        groupRepository.findGroupsByEstablishmentIdAndInitialsAppsIsContaining(
          establishmentIdFirst,
          AppType.PIN_PHONE_ADD_NEW_CONTACT,
        ).get(0).id,
      ),
    )

    val response = webTestClient.get()
      .uri("/v1/prisoners/${app.requestedBy}/apps/${app.id}?requestedBy=true")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectBody(object : ParameterizedTypeReference<AppResponseDto<String, Prisoner>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as AppResponseDto<String, Prisoner>
    val prisoner = response.requestedBy as Prisoner
    Assertions.assertEquals(app.appType, response.appType)
    Assertions.assertEquals(app.id, response.id)
    Assertions.assertEquals(app.status, response.status)
    Assertions.assertEquals(1, response.requests?.size)
    Assertions.assertEquals(app.requestedBy, prisoner.username)
    Assertions.assertNotNull(prisoner.firstName)
    Assertions.assertNotNull(prisoner.lastName)
    Assertions.assertEquals(app.assignedGroup.toString(), response.assignedGroup)
  }

  @Test
  fun `get an app by id with assigned group`() {
    val app = appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        requestedByFirst,
        LocalDateTime.now(ZoneOffset.UTC),
        requestedByFirstMainName,
        requestedByFirstSurname,
        AppStatus.PENDING,
        groupRepository.findGroupsByEstablishmentIdAndInitialsAppsIsContaining(
          establishmentIdFirst,
          AppType.PIN_PHONE_ADD_NEW_CONTACT,
        ).get(0).id,
      ),
    )

    val response = webTestClient.get()
      .uri("/v1/prisoners/${app.requestedBy}/apps/${app.id}?assignedGroup=true")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectBody(object : ParameterizedTypeReference<AppResponseDto<AssignedGroupDto, String>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as AppResponseDto<AssignedGroupDto, String>
    val assignedGroupDto = response.assignedGroup
    Assertions.assertEquals(app.appType, response.appType)
    Assertions.assertEquals(app.id, response.id)
    Assertions.assertEquals(app.requestedBy, response.requestedBy)
    Assertions.assertEquals(app.status, response.status)
    Assertions.assertEquals(1, response.requests?.size)
    Assertions.assertEquals(app.assignedGroup, assignedGroupDto.id)
    Assertions.assertNotNull(response.assignedGroup.name)
  }

  private fun populateEstablishments() {
    establishmentRepository.save(Establishment(establishmentIdFirst, "ESTABLISHMENT_NAME_1"))
    establishmentRepository.save(Establishment(establishmentIdSecond, "ESTABLISHMENT_NAME_2"))
    establishmentRepository.save(Establishment(establishmentIdThird, "ESTABLISHMENT_NAME_3"))
  }

  private fun populateGroups() {
    groupRepository.save(
      DataGenerator.generateGroups(
        assignedGroupFirst,
        establishmentIdFirst,
        assignedGroupFirstName,
        listOf(AppType.PIN_PHONE_ADD_NEW_CONTACT, AppType.PIN_PHONE_REMOVE_CONTACT),
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
        listOf(AppType.PIN_PHONE_ADD_NEW_CONTACT, AppType.PIN_PHONE_REMOVE_CONTACT),
        GroupType.WING,
      ),
    )
  }

  private fun populateApps() {
    appIdFirst = appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        requestedByFirst,
        LocalDateTime.now(ZoneOffset.UTC).minusDays(4),
        requestedByFirstMainName,
        requestedByFirstSurname,
        AppStatus.PENDING,
        assignedGroupFirst,
      ),
    ).id
    appIdSecond = appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        requestedByFirst,
        LocalDateTime.now(ZoneOffset.UTC).minusDays(2),
        requestedByFirstMainName,
        requestedByFirstSurname,
        AppStatus.PENDING,
        assignedGroupFirst,
      ),
    ).id

    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        requestedByFirst,
        LocalDateTime.now(ZoneOffset.UTC).minusDays(1),
        requestedByFirstMainName,
        requestedByFirstSurname,
        AppStatus.PENDING,
        assignedGroupFirst,
      ),
    )
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        AppType.PIN_PHONE_REMOVE_CONTACT,
        requestedBySecond,
        LocalDateTime.now(ZoneOffset.UTC).minusDays(2).minusHours(1),
        requestedBySecondMainName,
        requestedBySecondSurname,
        AppStatus.PENDING,
        assignedGroupFirst,
      ),
    )
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdSecond,
        AppType.PIN_PHONE_REMOVE_CONTACT,
        requestedByThird,
        LocalDateTime.now(ZoneOffset.UTC),
        requestedByFirstMainName,
        requestedByFirstSurname,
        AppStatus.PENDING,
        assignedGroupFirst,
      ),
    )
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS,
        requestedByThird,
        LocalDateTime.now(ZoneOffset.UTC),
        requestedByThirdMainName,
        requestedByThirdSurname,
        AppStatus.PENDING,
        assignedGroupSecond,
      ),
    )
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS,
        requestedByThird,
        LocalDateTime.now(ZoneOffset.UTC),
        requestedByThirdMainName,
        requestedByThirdSurname,
        AppStatus.PENDING,
        assignedGroupSecond,
      ),
    )
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdThird,
        AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS,
        requestedByThird,
        LocalDateTime.now(ZoneOffset.UTC),
        requestedByThirdMainName,
        requestedByThirdSurname,
        AppStatus.PENDING,
        assignedGroupSecond,
      ),
    )
  }
}
