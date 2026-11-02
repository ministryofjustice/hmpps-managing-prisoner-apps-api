package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "department")
data class Department(
  @Id
  val id: UUID,
  val slug: String,
  val name: String,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as Department
    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()
}
