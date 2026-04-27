package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.events

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.analytics.TelemetryService
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.StaffType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class PrisonerMergeServiceImpl(
  private val appRepository: AppRepository,
  private val telemetryService: TelemetryService,
  private val batchProcessor: PrisonerMergeBatchProcessor,
  @Value("\${hmpps.merge.page-size:50}")
  private val pageSize: Int,
) : PrisonerMergeService {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun mergePrisonerNomsNumbers(mergedNomsNumber: String, removedNomsNumber: String, description: String) {
    val createdOn: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)

    var totalMergedApps = 0
    var hasMoreApps = true
    var failedBatches = 0

    try {
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

        try {
          val batchCount = batchProcessor.updateBatch(appsPage, mergedNomsNumber, createdOn)
          totalMergedApps += batchCount
        } catch (e: Exception) {
          failedBatches++
          log.error("Failed to process batch $failedBatches for NOMS numbers $removedNomsNumber -> $mergedNomsNumber", e)
          // Rethrow to stop processing and send to DLQ
          throw RuntimeException("Prisoner merge failed after processing $totalMergedApps apps. Failed at batch $failedBatches", e)
        }

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
          "SUCCESS",
        )
        log.info("Merge completed successfully for $totalMergedApps apps for new NOMS number $mergedNomsNumber")
      }
    } catch (e: Exception) {
      telemetryService.addTelemetryDataForPrisonerMerge(
        Activity.PRISONER_ID_UPDATE,
        StaffType.MANAGE_APPS_ADMIN.toString(),
        createdOn,
        mergedNomsNumber,
        removedNomsNumber,
        "FAILED",
      )
      log.error("Prisoner merge failed for $removedNomsNumber -> $mergedNomsNumber. Successfully processed: $totalMergedApps apps", e)
      // Ensure RuntimeException is thrown for DLQ routing
      if (e is RuntimeException) {
        throw e
      } else {
        throw RuntimeException("Prisoner merge failed for $removedNomsNumber -> $mergedNomsNumber", e)
      }
    }
  }
}
