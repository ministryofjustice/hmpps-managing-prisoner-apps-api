package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.Column
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
  @Column(name = "message", length = 1000)
  val message: String,
  @Column(name = "created_date")
  val createdDate: LocalDateTime,
  @Column(name = "created_by")
  val createdBy: String,
  @Column(name = "app")
  val appId: UUID,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Comment

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()
}
