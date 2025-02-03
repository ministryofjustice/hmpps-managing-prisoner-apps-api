package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "groups")
data class Groups(
  @Id
  val id: UUID,
  val name: String,
  val establishment: UUID,
  @ElementCollection
  val staffs: Set<UUID>,
  @ElementCollection
  val initialsApps: List<AppType>,
  val type: GroupType,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Groups

    return id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}
