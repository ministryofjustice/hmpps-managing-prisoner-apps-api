package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto

import com.fasterxml.jackson.annotation.JsonFormat
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import java.time.LocalDateTime
import java.util.*

data class AppResponseDto(
  val id: UUID?,
  val reference: String,
  val assignedGroup: Any,
  val appType: AppType,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val requestedDate: LocalDateTime,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val createdDate: LocalDateTime,
  val createdBy: String,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val lastModifiedDate: LocalDateTime?,
  val lastModifiedBy: String?,
  val comments: List<UUID>?,
  val requests: List<Map<String, Any>>?,
  val requestedBy: Any,
  val requestedByFullName: String,
  val status: AppStatus,
)
