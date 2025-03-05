package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import java.time.LocalDateTime
import java.util.*

data class AppResponseDto(
  val id: UUID?,
  val reference: String,
  val assignedGroup: AssignedGroupDto,
  val appType: AppType,
  val createdDate: LocalDateTime,
  val lastModifiedDateTime: LocalDateTime,
  val lastModifiedBy: UUID,
  val comments: List<UUID>?,
  val requests: List<Map<String, Any>>?,
  val requestedDateTime: LocalDateTime,
  val requestedBy: Any,
)
