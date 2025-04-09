package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto

import java.time.LocalDateTime
import java.util.*

data class CommentResponseDto<T>(
  val id: UUID,
  val appId: UUID,
  val message: String,
  val prisonerNumber: String,
  val createdDate: LocalDateTime,
  val createdBy: T,
)
