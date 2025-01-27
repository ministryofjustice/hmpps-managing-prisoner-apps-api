package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.Id
import java.time.LocalDateTime
import java.util.UUID

data class Comment(
  @Id
  val id: UUID,
  val message: String,
  val createdDate: LocalDateTime,
  val createdBy: UUID,
  val users: Set<User>,
  val app: UUID
  )
