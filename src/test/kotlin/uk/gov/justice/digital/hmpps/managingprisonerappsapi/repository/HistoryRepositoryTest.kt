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
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.History
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@SpringBootTest(classes = [HistoryRepository::class])
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = ["uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository"])
@EntityScan("uk.gov.justice.digital.hmpps.managingprisonerappsapi.model")
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
class HistoryRepositoryTest(@Autowired private val historyRepository: HistoryRepository) {

  @Test
  fun `save history`() {
    val id = UUID.randomUUID()
    val appId = UUID.randomUUID()
    val history = History(
      id,
      appId,
      EntityType.APP,
      appId,
      Activity.APP_SUBMITTED,
      "ETS",
      "A12345",
      LocalDateTime.now(ZoneOffset.UTC),
    )
    val entity = historyRepository.save(history)

    Assertions.assertNotNull(entity)

    val records = historyRepository.findByAppIdAndEstablishmentOrderByCreatedDate(appId, "ETS")
    Assertions.assertEquals(1, records.size)
  }
}
