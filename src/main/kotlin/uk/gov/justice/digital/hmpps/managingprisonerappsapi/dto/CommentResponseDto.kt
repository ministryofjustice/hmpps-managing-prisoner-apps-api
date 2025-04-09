package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime
import java.util.*

data class CommentResponseDto<T>(
  val id: UUID,
  val appId: UUID,
  val message: String,
  val prisonerNumber: String,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val createdDate: LocalDateTime,
  val createdBy: T,
)
