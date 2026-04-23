package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.events

import com.fasterxml.uuid.Generators
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.analytics.TelemetryService
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.History
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.StaffType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.HistoryRepository
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class PrisonerMergeServiceImpl(
  private val appRepository: AppRepository,
  private val historyRepository: HistoryRepository,
  private val telemetryService: TelemetryService,
  private val entityManager: EntityManager,
  @Value("\${hmpps.merge.page-size:50}")
  private val pageSize: Int,
) : PrisonerMergeService {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun mergePrisonerNomsNumbers(mergedNomsNumber: String, removedNomsNumber: String, description: String) {
    val createdOn: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)

    var totalMergedApps = 0
    var hasMoreApps = true

    // Process apps in pages.
    while (hasMoreApps) {
      val pageable = PageRequest.of(0, pageSize)
      val appsPage = appRepository.findAppsByRequestedBy(removedNomsNumber, pageable)

      if (appsPage.isEmpty) {
        if (totalMergedApps == 0) {
          log.info("No apps found for NOMS number $removedNomsNumber, skipping Merge")
        }
        break
      }

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
      }

      totalMergedApps += appsPage.content.size

      // Flush and clear the entity manager to free up memory after each page
      entityManager.flush()
      entityManager.clear()

      // Continue if there might be more apps
      hasMoreApps = appsPage.content.size == pageSize
    }

    if (totalMergedApps > 0) {
      telemetryService.addTelemetryDataForPrisonerMerge(
        Activity.PRISONER_ID_UPDATE,
        StaffType.MANAGE_APPS_ADMIN.toString(),
        createdOn,
        mergedNomsNumber,
        removedNomsNumber,
      )
      log.info("Merge completed for $totalMergedApps apps for new NOMS number $mergedNomsNumber")
    }
    return
  }
}
