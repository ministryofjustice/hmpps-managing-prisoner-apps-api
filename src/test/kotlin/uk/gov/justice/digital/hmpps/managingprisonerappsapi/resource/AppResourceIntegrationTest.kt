package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppsSearchQueryDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AssignedGroupDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class AppResourceIntegrationTest(
  @Autowired private val appRepository: AppRepository,
  @Autowired private val groupRepository: GroupRepository,
  @Autowired private val establishmentRepository: EstablishmentRepository,
) : IntegrationTestBase() {

  /*@LocalServerPort
  private val port = 0

  private val baseUrl = "http://localhost"*/

  val establishmentIdFirst = "TEST_ESTABLISHMENT_FIRST"
  val establishmentIdSecond = "TEST_ESTABLISHMENT_SECOND"
  val establishmentIdThird = "TEST_ESTABLISHMENT_THIRD"
  val assignedGroupFirst = UUID.randomUUID()
  val assignedGroupFirstName = "Business Hub"
  val assignedGroupSecond = UUID.randomUUID()
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

  lateinit var appIdFirst: UUID
  lateinit var appIdSecond: UUID

  @BeforeEach
  fun setUp() {
    appRepository.deleteAll()
    groupRepository.deleteAll()
    establishmentRepository.deleteAll()

    populateEstablishments()
    populateGroups()
    populateApps()

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
    val response = webTestClient.post()
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
    Assertions.assertEquals(1, response.requests?.size)
  }

  @Test
  fun `search apps by query filters`() {
    val searchQueryDto = AppsSearchQueryDto(
      1,
      5,
      setOf(AppStatus.PENDING),
      setOf(
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        AppType.PIN_PHONE_REMOVE_CONTACT,
        AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS,
      ),
      requestedByFirst,
      setOf(assignedGroupFirst, assignedGroupSecond),
    )
    webTestClient.post()
      .uri("/v1//prisoners/apps/search")
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
    val response = webTestClient.get()
      .uri("/v1/apps/$appIdFirst/forward/groups/$assignedGroupSecond")
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
    Assertions.assertEquals(assignedGroupSecond, response.assignedGroup.id)
  }

  @Test
  fun `submit an app with no roles`() {
    webTestClient.post()
      .uri("/v1/prisoners/G12345/apps")
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
        establishmentIdSecond,
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
        establishmentIdSecond,
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
