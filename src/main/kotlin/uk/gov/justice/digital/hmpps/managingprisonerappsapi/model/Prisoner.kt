package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
data class Prisoner(
  @Id
  val id: String,
  val firstName: String,
  val lastName: String,
  val category: UserCategory,
  val location: String,
  val iep: String,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Prisoner

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()
}
