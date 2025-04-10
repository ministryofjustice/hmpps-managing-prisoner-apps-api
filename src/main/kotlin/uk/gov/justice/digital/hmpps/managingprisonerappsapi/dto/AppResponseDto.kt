package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto

import com.fasterxml.jackson.annotation.JsonFormat
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import java.time.LocalDateTime
import java.util.*

data class AppResponseDto<X, Y>(
  val id: UUID?,
  val reference: String?,
  val assignedGroup: X,
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
  val requestedBy: Y,
  val requestedByFirstName: String,
  val requestedByLastName: String,
  val status: AppStatus,
  val establishmentId: String,
  val responses: List<UUID>?,
)
