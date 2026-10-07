package uk.gov.justice.digital.hmpps.managingprisonerappsapi.analytics

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class TelemetryService(private var telemetryClient: TelemetryClient) {

  companion object {
    private val FORMATTER = DateTimeFormatter.ISO_DATE_TIME
    private val logger = LoggerFactory.getLogger(TelemetryService::class.java)
  }

  fun addTelemetryData(
    entityId: UUID,
    entityType: EntityType,
    appId: UUID,
    activity: Activity,
    establishment: String,
    createdBy: String,
    createdDate: LocalDateTime,
    prisonerId: String,
    appType: Long,
    applicationGroup: Long,
    department: String,
    rejectionReason: String?,
  ) {
    try {
      val map = LinkedHashMap<String, String>()
      map["requestedBy"] = prisonerId
      map["appId"] = appId.toString()
      map["appType"] = appType.toString()
      map["appGroup"] = applicationGroup.toString()
      map["dateTime"] = createdDate.format(FORMATTER)
      map["createdBy"] = createdBy
      map["establishment"] = establishment
      map["department"] = department
      map["rejectionReason"] = rejectionReason.toString()

      telemetryClient.trackEvent(activity.toString(), map, null)
    } catch (e: Exception) {
      logger.error("Issue sending telemetry data: ${e.message}")
    }
  }

  fun addTelemetryDataForPrisonerEvent(
    activity: Activity,
    createdBy: String,
    createdDate: LocalDateTime,
    newPrisonerId: String,
    additionalData: String,
    status: String,
  ) {
    try {
      val map = LinkedHashMap<String, String>()

      map["dateTime"] = createdDate.format(FORMATTER)
      map["createdBy"] = createdBy
      map["newPrisoneId"] = newPrisonerId
      map["status"] = status

      when (activity) {
        Activity.PRISONER_ID_UPDATE ->
          map["removedPrisoneId"] = additionalData
        Activity.PRISONER_RELEASED ->
          map["releaseReason"] = additionalData
        else -> {
          logger.error("Invalid activity type for prisoner event telemetry: $activity")
          return
        }
      }

      telemetryClient.trackEvent(activity.toString(), map, null)
    } catch (e: Exception) {
      logger.error("Issue sending merge telemetry data: ${e.message}")
    }
  }
}
