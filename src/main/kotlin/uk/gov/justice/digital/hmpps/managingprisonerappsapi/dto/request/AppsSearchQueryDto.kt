package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import java.util.*

data class AppsSearchQueryDto(
  val page: Long,
  val size: Long = 20,
  val status: Set<AppStatus>,
  var applicationTypes: Set<Long>? = null,
  val requestedBy: String? = null,
  var assignedGroups: Set<UUID>? = null,
  val firstNightCenter: Boolean? = null,
)
