package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.model

import java.time.LocalDateTime
import java.util.UUID

data class Comment(
  val id: UUID,
  val message: String,
  val createdDate: LocalDateTime,
  val createdBy: UUID,
  val users: Set<User>,
  val app: UUID
  )
