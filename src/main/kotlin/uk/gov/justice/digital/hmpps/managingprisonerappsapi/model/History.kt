package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import java.time.LocalDateTime
import java.util.UUID

@Entity
data class History(
  @Id
  val id: UUID,
  val entityId: UUID,
  @Enumerated(EnumType.STRING)
  val entityType: EntityType,
  val appId: UUID,
  @Enumerated(EnumType.STRING)
  val activity: Activity,
  val establishment: String,
  val createdBy: String,
  val createdDate: LocalDateTime,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as History

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()
}
