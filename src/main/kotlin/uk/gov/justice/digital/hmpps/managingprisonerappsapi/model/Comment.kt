package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "comment")
data class Comment(
  @Id
  val id: UUID,
  @Column(name = "message")
  val message: String,
  @Column(name = "created_date")
  val createdDate: LocalDateTime,
  @Column(name = "created_by")
  val createdBy: UUID,
  @ElementCollection
  val users: Set<UUID>,
  @Column(name = "app")
  val app: UUID,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Comment

    return id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}
