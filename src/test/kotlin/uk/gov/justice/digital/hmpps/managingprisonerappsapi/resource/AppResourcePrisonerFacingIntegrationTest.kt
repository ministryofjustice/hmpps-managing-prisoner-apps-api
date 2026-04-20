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
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppResponsePrisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.ApplicationGroupResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrisonerAppsPage
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.PrisonerSearchApiExtension.Companion.prisonerSearchApi
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationGroup
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppFileRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationGroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationTypeRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator.Companion.CONTACT_NUMBER
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class AppResourcePrisonerFacingIntegrationTest(
  @Autowired private val appRepository: AppRepository,
  @Autowired private val groupRepository: GroupRepository,
  @Autowired private val establishmentRepository: EstablishmentRepository,
  @Autowired private val applicationGroupRepository: ApplicationGroupRepository,
  @Autowired private val applicationTypeRepository: ApplicationTypeRepository,
  @Autowired private val appFileRepository: AppFileRepository,
) : IntegrationTestBase() {

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

  lateinit var appIdFirst: UUID
  lateinit var appIdSecond: UUID

  @BeforeEach
  fun setUp() {
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
  fun `submit an app by prisoner and get app by id`() {
    var response = webTestClient.post()
      .uri("/v1/prisoners/apps")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
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
      .expectBody(object : ParameterizedTypeReference<AppResponsePrisoner<Any, Prisoner>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as AppResponsePrisoner<Any, Prisoner>

    Assertions.assertEquals(applicationTypeOne, response.applicationType.id)
    Assertions.assertEquals(loggedUserId, response.requestedBy.userId)
    Assertions.assertEquals(AppStatus.PENDING, response.status)
    Assertions.assertEquals(1, response.requests.size)
    Assertions.assertNotNull(response.requests.get(0)["id"])
    Assertions.assertEquals(response.requests.get(0)["contact-number"], CONTACT_NUMBER)

    // Get app by id
    response = webTestClient.get()
      .uri("/v1/prisoners/apps/${response.id}")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header("Content-Type", "application/json")
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(object : ParameterizedTypeReference<AppResponsePrisoner<Any, Prisoner>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as AppResponsePrisoner<Any, Prisoner>

    Assertions.assertNotNull(response)

    // get apps
    val appsResponse = webTestClient.get()
      .uri("/v1/prisoners/apps?pageNum=1")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
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
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
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
    // val applicationGroupOne = ApplicationGroup(applicationGroupOne, applicationGroupOneName, listOf(addSocialContact, removeContact, addOfficialContact, addGenericPinPhoneEnquiry))

    // applicationGroupRepository.save<ApplicationGroup>(applicationGroupOne)
    // addOfficialContact.applicationGroup = applicationGroupOne
    // applicationTypeRepository.save(addOfficialContact)
    val appgrp = applicationTypeRepository.findById(applicationTypeOne)
    // println(appgrp)
  }

  protected fun populateApps() {
    appIdFirst = appRepository.save(
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
    ).id
    appIdSecond = appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        null,
        applicationTypeTwo,
        applicationGroupOne,
        requestedByFirst,
        LocalDateTime.now(ZoneOffset.UTC).minusDays(2),
        requestedByFirstMainName,
        requestedByFirstSurname,
        AppStatus.PENDING,
        assignedGroupFirst,
        false,
      ),
    ).id

    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        null,
        applicationTypeThree,
        applicationGroupOne,
        requestedByFirst,
        LocalDateTime.now(ZoneOffset.UTC).minusDays(1),
        requestedByFirstMainName,
        requestedByFirstSurname,
        AppStatus.PENDING,
        assignedGroupFirst,
        false,
      ),
    )
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        null,
        applicationTypeFour,
        applicationGroupOne,
        requestedBySecond,
        LocalDateTime.now(ZoneOffset.UTC).minusDays(2).minusHours(1),
        requestedBySecondMainName,
        requestedBySecondSurname,
        AppStatus.PENDING,
        assignedGroupFirst,
        false,
      ),
    )
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdSecond,
        null,
        applicationTypeOne,
        applicationGroupOne,
        requestedByThird,
        LocalDateTime.now(ZoneOffset.UTC),
        requestedByFirstMainName,
        requestedByFirstSurname,
        AppStatus.PENDING,
        assignedGroupFirst,
        false,
      ),
    )
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        null,
        applicationTypeOne,
        applicationGroupOne,
        requestedByThird,
        LocalDateTime.now(ZoneOffset.UTC),
        requestedByThirdMainName,
        requestedByThirdSurname,
        AppStatus.PENDING,
        assignedGroupSecond,
        false,
      ),
    )
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        null,
        applicationTypeOne,
        applicationGroupOne,
        requestedByThird,
        LocalDateTime.now(ZoneOffset.UTC),
        requestedByThirdMainName,
        requestedByThirdSurname,
        AppStatus.PENDING,
        assignedGroupSecond,
        false,
      ),
    )
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdThird,
        null,
        applicationTypeOne,
        applicationGroupOne,
        requestedByThird,
        LocalDateTime.now(ZoneOffset.UTC),
        requestedByThirdMainName,
        requestedByThirdSurname,
        AppStatus.PENDING,
        assignedGroupSecond,
        false,
      ),
    )
  }
}
