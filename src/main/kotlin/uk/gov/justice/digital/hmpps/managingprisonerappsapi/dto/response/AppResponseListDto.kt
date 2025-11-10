package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime
import java.util.*

data class AppResponseListDto(
  val page: Int,
  val totalRecords: Long,
  val exhausted: Boolean,
  val applicationTypes: Map<Long, ApplicationTypeResponse>,
  val assignedGroups: List<GroupAppListViewDto>,
  val firstNightCenter: Long,
  val apps: List<AppListViewDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GroupAppListViewDto(
  val id: UUID,
  val name: String,
  val count: Long?,
)

data class AppListViewDto(
  val id: UUID,
  val establishmentId: String,
  val status: String,
  val appType: ApplicationTypeResponse,
  val requestedBy: String,
  val requestedByFirstName: String,
  val requestedByLastName: String,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val createdDate: LocalDateTime,
  val assignedGroup: Any,
)
