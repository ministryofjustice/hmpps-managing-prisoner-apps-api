package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import java.util.*

data class AppsSearchQueryDto(
  val page: Long,
  val size: Long = 20,
  val status: Set<AppStatus>,
  var appTypes: Set<AppType>? = null,
  val requestedBy: String? = null,
  var assignedGroups: Set<UUID>? = null,
)
