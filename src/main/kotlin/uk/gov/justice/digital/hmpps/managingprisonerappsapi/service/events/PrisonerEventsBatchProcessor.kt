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
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Decision
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.History
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Response
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.StaffType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.HistoryRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ResponseRepository
import java.time.LocalDateTime

@Component
class PrisonerEventsBatchProcessor(
  private val appRepository: AppRepository,
  private val historyRepository: HistoryRepository,
  private val entityManager: EntityManager,
  private val responseRepository: ResponseRepository,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    const val RELEASED_REJECTION_COMMENT = "Prisoner has left the establishment"
  }

  @Transactional
  fun updateBatchForMerge(appsPage: Page<App>, mergedNomsNumber: String, removedNomsNumber: String, createdOn: LocalDateTime): Int {
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
          removedNomsNumber,
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

  /**
   * Steps :
   * Set all open apps for the prisoner to Rejected
   * Add a Response for each app with the Rejection reason
   * Add an entry in History table
   */
  @Transactional
  fun updateBatchForPrisonerRelease(appsPage: Page<App>, nomsNumber: String, releaseReason: String, createdOn: LocalDateTime): Int {
    try {
      var updatedCount = 0

      appsPage.content.forEach { app ->
        // Update app's status to REJECTED
        app.status = AppStatus.REJECTED
        appRepository.save(app)

        // Create a new Response
        val responseEntity = responseRepository.save(
          Response(
            Generators.timeBasedEpochGenerator().generate(),
            RELEASED_REJECTION_COMMENT,
            Decision.REJECTED,
            createdOn,
            StaffType.MANAGE_APPS_ADMIN.toString(),
            app.id,
          ),
        )

        // Create new history entry
        historyRepository.save(
          History(
            Generators.timeBasedEpochGenerator().generate(),
            responseEntity.id,
            EntityType.RESPONSE,
            app.id,
            Activity.PRISONER_RELEASED,
            app.establishmentId,
            StaffType.MANAGE_APPS_ADMIN.toString(),
            createdOn,
            releaseReason,
          ),
        )
        updatedCount++
      }

      // Flush and clear the entity manager to free up memory after each batch
      entityManager.flush()
      entityManager.clear()
      log.info("Batch processed: $updatedCount apps updated for NOMS number $nomsNumber")
      return updatedCount
    } catch (e: Exception) {
      log.error("Error processing batch for NOMS number $nomsNumber", e)
      throw RuntimeException("Failed to process batch for prisoner release: $nomsNumber", e)
    }
  }
}
