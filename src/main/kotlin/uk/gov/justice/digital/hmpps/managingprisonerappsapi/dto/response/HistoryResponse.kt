package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response

import com.fasterxml.jackson.annotation.JsonFormat
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import java.time.LocalDateTime
import java.util.UUID

data class HistoryResponse(
  val id: UUID,
  val appId: UUID,
  val entityId: UUID,
  val entityType: EntityType,
  val activityMessage: String,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val createdDate: LocalDateTime,
)
