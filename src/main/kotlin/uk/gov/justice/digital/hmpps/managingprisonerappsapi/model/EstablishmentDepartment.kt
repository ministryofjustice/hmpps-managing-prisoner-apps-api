package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "establishment_departments")
data class EstablishmentDepartment(

  @Id
  val id: UUID,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
    name = "establishment_id",
    nullable = false,
    foreignKey = ForeignKey(name = "establishment_departments_establishment_fk"),
  )
  val establishment: Establishment,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
    name = "department_id",
    nullable = false,
    foreignKey = ForeignKey(name = "establishment_departments_department_fk"),
  )
  val department: Department,

  val active: Boolean,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as EstablishmentDepartment
    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()
}
