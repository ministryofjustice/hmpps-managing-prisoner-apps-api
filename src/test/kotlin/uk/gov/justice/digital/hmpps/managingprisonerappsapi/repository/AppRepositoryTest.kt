package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.junit.jupiter.api.Assertions
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
import java.util.*

@SpringBootTest(classes = [AppRepository::class])
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = ["uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository"])
@EntityScan("uk.gov.justice.digital.hmpps.managingprisonerappsapi.model")
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
class AppRepositoryTest(@Autowired val appRepository: AppRepository) {
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
      createdApp.requestedByFullName,
      createdApp.status,
      UUID.randomUUID().toString(),
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
    val assignedGroupFirst = UUID.randomUUID()
    val assignedGroupSecond = UUID.randomUUID()
    val requestedByFirst = "A12345"
    val requestedByFirstFullName= "John Smith"
    val requestedBySecondFullName= "John Butler"
    val requestedBySecond = "B12345"
    val requestedByThird = "C12345"
    val requestedByThirdFullName = "Test User"
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        requestedByFirst,
        requestedByFirstFullName,
        AppStatus.PENDING,
        assignedGroupFirst,
      ),
    )
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        requestedByFirst,
        requestedByFirstFullName,
        AppStatus.PENDING,
        assignedGroupFirst,
      ),
    )
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        requestedByFirst,
        requestedByFirstFullName,
        AppStatus.PENDING,
        assignedGroupFirst,
      ),
    )
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        AppType.PIN_PHONE_REMOVE_CONTACT,
        requestedBySecond,
        requestedBySecondFullName,
        AppStatus.PENDING,
        assignedGroupFirst,
      ),
    )
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdSecond,
        AppType.PIN_PHONE_REMOVE_CONTACT,
        requestedByThird,
        requestedByThirdFullName,
        AppStatus.PENDING,
        assignedGroupFirst,
      ),
    )
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdSecond,
        AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS,
        requestedByThird,
        requestedByThirdFullName,
        AppStatus.PENDING,
        assignedGroupSecond,
      ),
    )
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdSecond,
        AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS,
        requestedByThird,
        requestedByThirdFullName,
        AppStatus.PENDING,
        assignedGroupSecond,
      ),
    )
    appRepository.save(
      DataGenerator.generateApp(
        establishmentIdThird,
        AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS,
        requestedByThird,
        requestedByThirdFullName,
        AppStatus.PENDING,
        assignedGroupSecond,
      ),
    )

    // By establishment Id and status
    var countResult = appRepository.countBySearchFilter(
      establishmentIdFirst,
      setOf(AppStatus.PENDING),
      null,
      null,
      null,
    )
    Assertions.assertEquals(2, countResult.size)
    Assertions.assertEquals(3, countResult.get(0).getCount())
    Assertions.assertEquals(1, countResult.get(1).getCount())
    Assertions.assertEquals(AppType.PIN_PHONE_ADD_NEW_CONTACT, countResult.get(0).getAppType())
    Assertions.assertEquals(AppType.PIN_PHONE_REMOVE_CONTACT, countResult.get(1).getAppType())

    var pageResult = appRepository.appsBySearchFilter(
      establishmentIdFirst,
      setOf(AppStatus.PENDING),
      null,
      null,
      null,
      PageRequest.of(0, 4),
    )
    Assertions.assertEquals(pageResult.totalElements, 4)

    // By establishment and status and appType
    countResult = appRepository.countBySearchFilter(
      establishmentIdSecond,
      setOf(AppStatus.PENDING),
      setOf(AppType.PIN_PHONE_REMOVE_CONTACT),
      null,
      null,
    )
    Assertions.assertEquals(1, countResult.size)
    Assertions.assertEquals(1, countResult.get(0).getCount())
    Assertions.assertEquals(AppType.PIN_PHONE_REMOVE_CONTACT, countResult.get(0).getAppType())

    pageResult = appRepository.appsBySearchFilter(
      establishmentIdSecond,
      setOf(AppStatus.PENDING),
      setOf(AppType.PIN_PHONE_REMOVE_CONTACT),
      null,
      null,
      PageRequest.of(0, 4),
    )
    Assertions.assertEquals(pageResult.totalElements, 1)

    // By EstablishmentId AND status and requestedBy
    countResult = appRepository.countBySearchFilter(
      establishmentIdFirst,
      setOf(AppStatus.PENDING),
      null,
      requestedByFirst,
      null,
    )
    Assertions.assertEquals(1, countResult.size)
    Assertions.assertEquals(3, countResult.get(0).getCount())
    Assertions.assertEquals(AppType.PIN_PHONE_ADD_NEW_CONTACT, countResult.get(0).getAppType())

    pageResult = appRepository.appsBySearchFilter(
      establishmentIdFirst,
      setOf(AppStatus.PENDING),
      null,
      requestedByFirst,
      null,
      PageRequest.of(0, 4),
    )
    Assertions.assertEquals(pageResult.totalElements, 3)


    // By EstablishmentId and Status and assignedGroup
    countResult = appRepository.countBySearchFilter(
      establishmentIdFirst,
      setOf(AppStatus.PENDING),
      null,
      null,
      setOf(assignedGroupFirst),
    )
    Assertions.assertEquals(2, countResult.size)
    pageResult = appRepository.appsBySearchFilter(
      establishmentIdFirst,
      setOf(AppStatus.PENDING),
      null,
      null,
      setOf(assignedGroupFirst),
      PageRequest.of(0, 4),
    )
    Assertions.assertEquals(pageResult.totalElements, 4)

    // By establishment id and status and appType and requestedBy and assignedGroup
    countResult = appRepository.countBySearchFilter(
      establishmentIdSecond,
      setOf(AppStatus.PENDING),
      setOf(AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS),
      requestedByThird,
      setOf(assignedGroupSecond),
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
      PageRequest.of(0, 4)
    )
    Assertions.assertEquals(2, pageResult1.totalElements)
    val userSearchResult = appRepository.searchRequestedByFullName(establishmentIdFirst, "john")
    Assertions.assertEquals(2, userSearchResult.size)
    Assertions.assertEquals("John Butler", userSearchResult.get(0).getFullName())
    Assertions.assertEquals("John Smith", userSearchResult.get(1).getFullName())
  }
}
