package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import com.fasterxml.uuid.Generators
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.analytics.TelemetryService
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.History
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.StaffType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.HistoryRepository
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class PrisonerMergeServiceImpl(
  private val appRepository: AppRepository,
  private val historyRepository: HistoryRepository,
  private val commentRepository: CommentRepository,
  private val telemetryService: TelemetryService,
) : PrisonerMergeService {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun mergePrisonerNomsNumbers(mergedNomsNumber: String, removedNomsNumber: String, description: String) {
    val createdOn: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
    var appsToMerge = appRepository.findAppsByRequestedBy(removedNomsNumber)

    if (appsToMerge.isEmpty()) {
      log.info("No apps found for NOMS number $removedNomsNumber, skipping merge")
      return
    }

    appsToMerge.forEach { app ->
      // Update app's requestedBy to mergedNomsNumber
      app.requestedBy = mergedNomsNumber
      appRepository.save(app)

      // Create new history entry for this app
      var history = History(
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

    telemetryService.addTelemetryDataForPrisonerMerge(
      Activity.PRISONER_ID_UPDATE,
      StaffType.MANAGE_APPS_ADMIN.toString(),
      createdOn,
      mergedNomsNumber,
      0,
      1,
    )
    log.info("Merge completed for $appsToMerge.size.toString() apps.")
    return
  }
}
