package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "response")
data class Response(
  @Id
  val id: UUID,
  val reason: String,
  val decision: Decision,
  val createdDate: LocalDateTime,
  val createdBy: UUID
  )
