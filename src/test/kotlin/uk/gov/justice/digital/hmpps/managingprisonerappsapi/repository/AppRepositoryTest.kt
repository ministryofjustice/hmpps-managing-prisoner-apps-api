package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
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
      createdApp.createdDate,
      createdApp.lastModifiedDateTime,
      createdApp.lastModifiedBy,
      createdApp.comments,
      createdApp.requestedDateTime,
      createdApp.requestedBy,
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
}
