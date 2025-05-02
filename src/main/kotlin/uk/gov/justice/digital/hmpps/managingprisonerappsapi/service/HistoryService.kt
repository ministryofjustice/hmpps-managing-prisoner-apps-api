package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.HistoryResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import java.util.UUID

interface HistoryService {

  fun getHistoryByAppId(appId: UUID, user: String): List<HistoryResponse>

  fun updateActivityInHistory(entityId: UUID, entityType: EntityType, appId: UUID, activity: Activity, establishment: String, createdBy: String)
}
