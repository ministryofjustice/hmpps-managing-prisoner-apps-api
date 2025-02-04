package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.LocalDateTime
import java.util.UUID

@Entity
data class App(
  @Id
  val id: UUID,
  val reference: String,
  val assignedGroup: UUID,
  val appType: AppType,
  val createdDate: LocalDateTime,
  val lastModifiedDateTime: LocalDateTime,
  val lastModifiedBy: UUID,
  @ElementCollection
  val comments: List<UUID>,
  // @ElementCollection
  // val requests: List<Map<String, JvmType.Object>>,
  val requestedDateTime: LocalDateTime,
  val requestedBy: UUID,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as App

    return id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}
