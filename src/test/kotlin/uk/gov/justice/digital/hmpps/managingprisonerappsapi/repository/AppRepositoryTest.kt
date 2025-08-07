package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import com.fasterxml.uuid.Generators
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@SpringBootTest(classes = [AppRepository::class])
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = ["uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository"])
@EntityScan("uk.gov.justice.digital.hmpps.managingprisonerappsapi.model")
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
class AppRepositoryTest(@Autowired val appRepository: AppRepository) {

  @BeforeEach
  fun setUp() {
    appRepository.deleteAll()
  }

  @AfterEach
  fun tearDown() {
    appRepository.deleteAll()
  }

  @Test
  fun `save app`() {
    val response = appRepository.save(DataGenerator.generateApp())
    Assertions.assertNotNull(response)
  }

  @Test
  fun `update app`() {
    val createdApp = appRepository.save(DataGenerator.generateApp())
    var app = App(
      createdApp.id,
      "new reference 123",
      createdApp.assignedGroup,
      createdApp.appType,
      createdApp.requestedDate,
      createdApp.createdDate,
      createdApp.createdBy,
      createdApp.lastModifiedDate,
      createdApp.lastModifiedBy,
      createdApp.comments,
      listOf(HashMap<String, Any>()),
      createdApp.requestedBy,
      createdApp.requestedByFirstName,
      createdApp.requestedByLastName,
      createdApp.status,
      Generators.timeBasedEpochGenerator().generate().toString(),
      mutableListOf(),
      false,
    )
    app = appRepository.save(app)
    Assertions.assertEquals("new reference 123", app.reference)
  }

  @Test
  fun `find app by id`() {
    val createdApp = appRepository.save(DataGenerator.generateApp())
    val app = appRepository.findById(createdApp.id)
    Assertions.assertEquals(true, app.isPresent)
  }

  @Test
  fun `delete app by id`() {
    val createdApp = appRepository.save(DataGenerator.generateApp())
    appRepository.deleteById(createdApp.id)
    val app = appRepository.findById(createdApp.id)
    Assertions.assertEquals(false, app.isPresent)
  }

  @Test
  fun `get apps by search filter`() {
    val establishmentIdFirst = "TEST_ESTABLISHMENT_FIRST"
    val establishmentIdSecond = "TEST_ESTABLISHMENT_SECOND"
    val establishmentIdThird = "TEST_ESTABLISHMENT_THIRD"
    val assignedGroupFirst = Generators.timeBasedEpochGenerator().generate()
    val assignedGroupSecond = Generators.timeBasedEpochGenerator().generate()
    val requestedByFirst = "A12345"
    val requestedByFirstMainName = "John"
    val requestedByFirstSurname = "Smith"
    val requestedBySecondMainName = "John"
    val requestedBySecondSurname = "Butler"
    val requestedBySecond = "B12345"
    val requestedByThird = "C12345"
    val requestedByThirdMainName = "Test"
    val requestedByThirdSurname = "User"
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT,
        requestedByFirst,
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
        AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT,
        requestedByFirst,
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
        AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT,
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
        AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT,
        requestedBySecond,
        LocalDateTime.now(ZoneOffset.UTC).minusDays(2),
        requestedBySecondMainName,
        requestedBySecondSurname,
        AppStatus.PENDING,
        assignedGroupFirst,
      ),
    )
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdSecond,
        AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT,
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

    // By establishment Id and status
    var countResult = appRepository.countBySearchFilterGroupByAppType(
      establishmentIdFirst,
      setOf(AppStatus.PENDING),
      null,
      null,
      null,
      false,
    )
    Assertions.assertEquals(1, countResult.size)
    Assertions.assertEquals(4, countResult.get(0).getCount())
    //   Assertions.assertEquals(1, countResult.get(1).getCount())
    Assertions.assertEquals(AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT, countResult.get(0).getAppType())
    //   Assertions.assertEquals(AppType.PIN_PHONE_ADD_NEW_CONTACT, countResult.get(1).getAppType())

    var countResultByAssignedGroup = appRepository.countBySearchFilterGroupByAssignedGroup(
      establishmentIdFirst,
      setOf(AppStatus.PENDING),
      null,
      null,
      null,
      false,
    )
    println("size by assignedgroup ${countResultByAssignedGroup.size}")
    println(countResultByAssignedGroup.get(0).getCount())
    println(countResultByAssignedGroup.get(0).getAssignedGroup())

    var pageResult = appRepository.appsBySearchFilter(
      establishmentIdFirst,
      setOf(AppStatus.PENDING),
      null,
      null,
      null,
      false,
      PageRequest.of(0, 4),
    )
    Assertions.assertEquals(pageResult.totalElements, 4)

    // By establishment and status and appType
    countResult = appRepository.countBySearchFilterGroupByAppType(
      establishmentIdSecond,
      setOf(AppStatus.PENDING),
      setOf(AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT),
      null,
      null,
      false,
    )
    Assertions.assertEquals(1, countResult.size)
    Assertions.assertEquals(1, countResult.get(0).getCount())
    Assertions.assertEquals(AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT, countResult.get(0).getAppType())

    pageResult = appRepository.appsBySearchFilter(
      establishmentIdSecond,
      setOf(AppStatus.PENDING),
      setOf(AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT),
      null,
      null,
      false,
      PageRequest.of(0, 4),
    )
    Assertions.assertEquals(pageResult.totalElements, 1)

    // By EstablishmentId AND status and requestedBy
    countResult = appRepository.countBySearchFilterGroupByAppType(
      establishmentIdFirst,
      setOf(AppStatus.PENDING),
      null,
      requestedByFirst,
      null,
      false,
    )
    Assertions.assertEquals(1, countResult.size)
    Assertions.assertEquals(3, countResult.get(0).getCount())
    Assertions.assertEquals(AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT, countResult.get(0).getAppType())

    pageResult = appRepository.appsBySearchFilter(
      establishmentIdFirst,
      setOf(AppStatus.PENDING),
      null,
      requestedByFirst,
      null,
      false,
      PageRequest.of(0, 4),
    )
    Assertions.assertEquals(pageResult.totalElements, 3)

    // By EstablishmentId and Status and assignedGroup
    countResult = appRepository.countBySearchFilterGroupByAppType(
      establishmentIdFirst,
      setOf(AppStatus.PENDING),
      null,
      null,
      setOf(assignedGroupFirst),
      false,
    )
    Assertions.assertEquals(1, countResult.size)
    pageResult = appRepository.appsBySearchFilter(
      establishmentIdFirst,
      setOf(AppStatus.PENDING),
      null,
      null,
      setOf(assignedGroupFirst),
      false,
      PageRequest.of(0, 4),
    )
    Assertions.assertEquals(pageResult.totalElements, 4)

    // By establishment id and status and appType and requestedBy and assignedGroup
    countResult = appRepository.countBySearchFilterGroupByAppType(
      establishmentIdSecond,
      setOf(AppStatus.PENDING),
      setOf(AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS),
      requestedByThird,
      setOf(assignedGroupSecond),
      false,
    )
    Assertions.assertEquals(countResult.size, 1)
    Assertions.assertEquals(2, countResult.get(0).getCount())
    Assertions.assertEquals(AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS, countResult.get(0).getAppType())

    var pageResult1 = appRepository.appsBySearchFilter(
      establishmentIdSecond,
      setOf(AppStatus.PENDING),
      setOf(AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS),
      requestedByThird,
      setOf(assignedGroupSecond),
      false,
      PageRequest.of(0, 4),
    )
    Assertions.assertEquals(2, pageResult1.totalElements)
    val userSearchResult = appRepository.searchRequestedByFirstOrLastName(establishmentIdFirst, "john")
    Assertions.assertEquals(2, userSearchResult.size)
    Assertions.assertEquals("John", userSearchResult.get(0).getFirstName())
    Assertions.assertEquals("John", userSearchResult.get(1).getFirstName())
  }
}
