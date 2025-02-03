package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.*

@Entity
data class EstablishmentRole(
  @Id
  val id: UUID,
  val establishment: UUID,
  val role: Role,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as EstablishmentRole

    return id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}
