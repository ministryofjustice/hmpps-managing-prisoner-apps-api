package uk.gov.justice.digital.hmpps.managingprisonerappsapi.analytics

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
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

  fun addTelemetryData(entityId: UUID, entityType: EntityType, appId: UUID, activity: Activity, establishment: String, createdBy: String, createdDate: LocalDateTime, prisonerId: String, appType: Long) {
    try {
      val map = LinkedHashMap<String, String>()
      map["requestedBy"] = prisonerId
      map["appId"] = appId.toString()
      map["appType"] = appType.toString()
      map["dateTime"] = createdDate.format(FORMATTER)
      map["createdBy"] = createdBy
      map["establishment"] = establishment

      telemetryClient.trackEvent(activity.toString(), map, null)
    } catch (e: Exception) {
      logger.error("Issue sending telemetry data: ${e.message}")
    }
  }
}
