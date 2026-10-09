package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.events

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.analytics.TelemetryService
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.StaffType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import java.time.Clock
import java.time.LocalDateTime

@Service
class PrisonerEventServiceImpl(
  private val appRepository: AppRepository,
  private val telemetryService: TelemetryService,
  private val batchProcessor: PrisonerEventsBatchProcessor,
  private val clock: Clock = Clock.systemUTC(),
  @Value("\${hmpps.merge.page-size:50}")
  private val pageSize: Int,
) : PrisonerEventService {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun mergePrisonerNomsNumbers(mergedNomsNumber: String, removedNomsNumber: String, description: String) {
    log.info("Processing prisoner merge: $removedNomsNumber -> $mergedNomsNumber")
    processInBatches(
      activity = Activity.PRISONER_ID_UPDATE,
      primaryId = mergedNomsNumber,
      additionalData = removedNomsNumber,
      fetchPage = { pageable -> appRepository.findAppsByRequestedBy(removedNomsNumber, pageable) },
      processBatch = { page, createdOn -> batchProcessor.updateBatchForMerge(page, mergedNomsNumber, removedNomsNumber, createdOn) },
    )
  }

  override fun handlePrisonerReleased(nomsNumber: String, releaseReason: String, prisonId: String, eventTime: String) {
    log.info("Handling prisoner release for NOMS number $nomsNumber at prison $prisonId")
    processInBatches(
      activity = Activity.PRISONER_RELEASED,
      primaryId = nomsNumber,
      additionalData = releaseReason,
      fetchPage = { pageable -> appRepository.findOpenAppsForPrisoner(nomsNumber, pageable) },
      processBatch = { page, createdOn -> batchProcessor.updateBatchForPrisonerRelease(page, nomsNumber, releaseReason, createdOn) },
    )
  }

  override fun handlePrisonerReceived(nomsNumber: String, reason: String, prisonId: String, eventTime: String) {
    log.info("Handling prisoner received for NOMS number $nomsNumber at prison $prisonId with reason $reason")
    // Implement the logic to handle prisoner received
  }

  /**
   * Processes records in batches using page 0 repeatedly. This works because each batch
   * updates records so they no longer appear in subsequent queries (e.g. status changes,
   * NOMS number updates), naturally advancing through the full result set.
   */
  private fun processInBatches(
    activity: Activity,
    primaryId: String,
    additionalData: String,
    fetchPage: (Pageable) -> Page<App>,
    processBatch: (Page<App>, LocalDateTime) -> Int,
  ) {
    val createdOn = LocalDateTime.now(clock)
    var totalProcessed = 0
    var hasMoreApps = true

    try {
      while (hasMoreApps) {
        val page = fetchPage(PageRequest.of(0, pageSize))

        if (page.isEmpty) {
          if (totalProcessed == 0) log.info("No records found for $primaryId during $activity processing")
          break
        }
        totalProcessed += processBatch(page, createdOn)
        hasMoreApps = page.content.size == pageSize
      }

      if (totalProcessed > 0) {
        sendTelemetry(activity, primaryId, additionalData, "SUCCESS", createdOn)
        log.info("$activity completed successfully: $totalProcessed apps for $primaryId")
      }
    } catch (e: Exception) {
      sendTelemetry(activity, primaryId, additionalData, "FAILED", createdOn)
      log.error("$activity failed for $primaryId. Successfully processed: $totalProcessed apps", e)
      throw e as? RuntimeException ?: RuntimeException("$activity failed for $primaryId", e)
    }
  }

  private fun sendTelemetry(activity: Activity, primaryId: String, additionalData: String, status: String, createdOn: LocalDateTime) {
    telemetryService.addTelemetryDataForPrisonerEvent(
      activity,
      StaffType.MANAGE_APPS_ADMIN.toString(),
      createdOn,
      primaryId,
      additionalData,
      status,
    )
  }
}
