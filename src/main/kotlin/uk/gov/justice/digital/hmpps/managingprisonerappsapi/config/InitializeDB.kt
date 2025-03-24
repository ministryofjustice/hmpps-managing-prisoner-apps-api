package uk.gov.justice.digital.hmpps.managingprisonerappsapi.config


import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.init.DataSourceInitializer
import org.springframework.jdbc.datasource.init.DatabasePopulator
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import javax.sql.DataSource


@Configuration
class InitializeDB {

  /*@Bean
  fun initializeDatabase(datasource: DataSource) {
    val dataSourceInitializer = DataSourceInitializer()
    dataSourceInitializer.setDataSource(datasource)
    dataSourceInitializer.setDatabasePopulator(DatabasePopulator {  })
  }*/

  @Bean("app")
  fun appRepository(appRepository: AppRepository): AppRepository {
      val establishmentIdFirst = "TEST_ESTABLISHMENT_FIRST"
      val establishmentIdSecond = "TEST_ESTABLISHMENT_SECOND"
      val establishmentIdThird = "TEST_ESTABLISHMENT_THIRD"
      val assignedGroupFirst = UUID.randomUUID()
      val assignedGroupFirstName = "Business Hub"
      val assignedGroupSecond = UUID.randomUUID()
      val assignedGroupSecondName = "OMU"
      val requestedByFirst = "A12345"
      val requestedByFirstFullName= "John Smith"
      val requestedBySecondFullName= "John Butler"
      val requestedBySecond = "B12345"
      val requestedByThird = "C12345"
      val requestedByThirdFullName = "Test User"
      appRepository.save(
        generateApp(
          establishmentIdFirst,
          AppType.PIN_PHONE_ADD_NEW_CONTACT,
          requestedByFirst,
          requestedByFirstFullName,
          AppStatus.PENDING,
          assignedGroupFirst,
        ),
      )
      appRepository.save(
        generateApp(
          establishmentIdFirst,
          AppType.PIN_PHONE_ADD_NEW_CONTACT,
          requestedByFirst,
          requestedByFirstFullName,
          AppStatus.PENDING,
          assignedGroupFirst,
        ),
      )
      appRepository.save(
        generateApp(
          establishmentIdFirst,
          AppType.PIN_PHONE_ADD_NEW_CONTACT,
          requestedByFirst,
          requestedByFirstFullName,
          AppStatus.PENDING,
          assignedGroupFirst,
        ),
      )
      appRepository.save(
        generateApp(
          establishmentIdFirst,
          AppType.PIN_PHONE_REMOVE_CONTACT,
          requestedBySecond,
          requestedBySecondFullName,
          AppStatus.PENDING,
          assignedGroupFirst,
        ),
      )
      appRepository.save(
        generateApp(
          establishmentIdSecond,
          AppType.PIN_PHONE_REMOVE_CONTACT,
          requestedByThird,
          requestedByThirdFullName,
          AppStatus.PENDING,
          assignedGroupFirst,
        ),
      )
      appRepository.save(
        generateApp(
          establishmentIdSecond,
          AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS,
          requestedByThird,
          requestedByThirdFullName,
          AppStatus.PENDING,
          assignedGroupSecond,
        ),
      )
      appRepository.save(
        generateApp(
          establishmentIdSecond,
          AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS,
          requestedByThird,
          requestedByThirdFullName,
          AppStatus.PENDING,
          assignedGroupSecond,
        ),
      )
      appRepository.save(
        generateApp(
          establishmentIdThird,
          AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS,
          requestedByThird,
          requestedByThirdFullName,
          AppStatus.PENDING,
          assignedGroupSecond,
        ),
      )
    return appRepository
  }

  private fun generateApp(establishmentId: String, appType: AppType, requestedBy: String, requestedByFullName: String, appStatus: AppStatus, groupId: UUID): App = App(
    UUID.randomUUID(),
    UUID.randomUUID().toString(),
    groupId,
    appType,
    LocalDateTime.now(ZoneOffset.UTC),
    LocalDateTime.now(ZoneOffset.UTC),
    "testStaaf@moj",
    LocalDateTime.now(ZoneOffset.UTC),
    "testStaaf@moj",
    arrayListOf(UUID.randomUUID()),
    listOf(HashMap<String, Any>().apply { put("contact", 123456) }),
    requestedBy,
    requestedByFullName,
    appStatus,
    establishmentId,
  )
}