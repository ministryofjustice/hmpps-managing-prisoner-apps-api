package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.events

import com.fasterxml.uuid.Generators
import jakarta.persistence.EntityManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.History
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.StaffType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.HistoryRepository
import java.time.LocalDateTime

@Component
class PrisonerMergeBatchProcessor(
  private val appRepository: AppRepository,
  private val historyRepository: HistoryRepository,
  private val entityManager: EntityManager,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun updateBatch(appsPage: Page<App>, mergedNomsNumber: String, createdOn: LocalDateTime): Int {
    try {
      var updatedCount = 0

      appsPage.content.forEach { app ->
        // Update app's requestedBy to mergedNomsNumber
        app.requestedBy = mergedNomsNumber
        appRepository.save(app)

        // Create new history entry for this app
        val history = History(
          Generators.timeBasedEpochGenerator().generate(),
          app.id,
          EntityType.APP,
          app.id,
          Activity.PRISONER_ID_UPDATE,
          app.establishmentId,
          StaffType.MANAGE_APPS_ADMIN.toString(),
          createdOn,
        )
        historyRepository.save(history)
        updatedCount++
      }

      // Flush and clear the entity manager to free up memory after each batch
      entityManager.flush()
      entityManager.clear()

      log.info("Batch processed: $updatedCount apps updated for NOMS number $mergedNomsNumber")
      return updatedCount
    } catch (e: Exception) {
      log.error("Error processing batch for NOMS number $mergedNomsNumber", e)
      throw RuntimeException("Failed to process batch for prisoner merge: $mergedNomsNumber", e)
    }
  }
}
