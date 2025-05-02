package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import java.util.*

@Service
class ActivityService(
  private val historyService: HistoryService,
) {

  fun addActivity(entityId: UUID, entityType: EntityType, appId: UUID, activity: Activity, establishment: String, createdBy: String) {
    historyService.updateActivityInHistory(entityId, entityType, appId, activity, establishment, createdBy)
  }
}
