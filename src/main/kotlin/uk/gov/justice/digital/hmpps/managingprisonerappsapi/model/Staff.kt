package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.*

@Entity
data class Staff(
  @Id
  val id: UUID,
  val firstName: String,
  val lastName: String,
  val category: UserCategory,
  @ElementCollection
  val roles: Set<UUID>,
  val jobTitle: String,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Staff

    return id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}
