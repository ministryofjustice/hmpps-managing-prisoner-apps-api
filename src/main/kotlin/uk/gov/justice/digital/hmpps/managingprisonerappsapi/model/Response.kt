package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "response")
data class Response(
  @Id
  val id: UUID,
  @Column(name = "reason", length = 1000)
  val reason: String,
  @Enumerated(EnumType.STRING)
  val decision: Decision,
  val createdDate: LocalDateTime,
  val createdBy: String,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Response

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()
}
