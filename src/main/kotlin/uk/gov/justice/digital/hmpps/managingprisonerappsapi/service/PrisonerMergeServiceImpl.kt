package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import com.fasterxml.uuid.Generators
import com.microsoft.applicationinsights.TelemetryClient
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.History
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.HistoryRepository
import java.time.LocalDateTime
import java.time.ZoneOffset

const val MERGE_EVENT_NAME = "manageapps-api-prisoner.merged"
const val CREATED_BY = "USER1"

@Service
class PrisonerMergeServiceImpl(
  private val appRepository: AppRepository,
  private val historyRepository: HistoryRepository,
  private val commentRepository: CommentRepository,
  private val telemetryClient: TelemetryClient,
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

      // Create new comment for this app request
      var comment = Comment(
        Generators.timeBasedEpochGenerator().generate(),
        "Prisoner merged from $removedNomsNumber to $mergedNomsNumber. Description: $description",
        createdOn,
        CREATED_BY,
        app.id,
      )
      commentRepository.save(comment)

      // Create new history entry for this app
      var history = History(
        Generators.timeBasedEpochGenerator().generate(),
        comment.id,
        EntityType.ASSIGNED_GROUP,
        app.id,
        Activity.APP_SUBMITTED,
        app.establishmentId,
        CREATED_BY,
        createdOn,
      )
      historyRepository.save(history)
    }

    telemetryClient.trackEvent(
      MERGE_EVENT_NAME,
      mapOf(
        "MERGE-FROM" to removedNomsNumber,
        "MERGE-TO" to mergedNomsNumber,
        "APPS-MERGED" to appsToMerge.size.toString(),
      ),
      null,
    )
    log.info("Merge completed for $appsToMerge.size.toString() apps.")
    return
  }
}
