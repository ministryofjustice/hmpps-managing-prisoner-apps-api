package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.model

import java.time.LocalDateTime
import java.util.UUID

data class Response(
  val id: UUID,
  val reason: String,
  val decision: Decision,
  val createdDate: LocalDateTime,
  val createdBy: UUID
  )
