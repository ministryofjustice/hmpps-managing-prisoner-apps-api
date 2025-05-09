package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "establishment")
data class Establishment(
  @Id
  val id: String,
  val name: String,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Establishment

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()
}
