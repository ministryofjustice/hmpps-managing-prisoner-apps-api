package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.analytics.TelemetryService
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import java.time.LocalDateTime
import java.util.*

@Service
class ActivityService(
  private val historyService: HistoryService,
  private val telemetryService: TelemetryService,
) {

  fun addActivity(
    entityId: UUID,
    entityType: EntityType,
    appId: UUID,
    activity: Activity,
    establishment: String,
    createdBy: String,
    createdDate: LocalDateTime,
    prisonerId: String,
    appType: AppType,
  ) {
    historyService.updateActivityInHistory(entityId, entityType, appId, activity, establishment, createdBy, createdDate)
    telemetryService.addTelemetryData(entityId, entityType, appId, activity, establishment, createdBy, createdDate, prisonerId, appType)
  }
}
