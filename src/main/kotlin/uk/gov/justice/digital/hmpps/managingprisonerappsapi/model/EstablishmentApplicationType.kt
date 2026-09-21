package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
  name = "establishment_application_type",
  uniqueConstraints = [
    // this constraint now enforces that
    // an establishment can only be configured once for a given application type.
    UniqueConstraint(
      name = "uq_est_app_type_establishment_type",
      columnNames = ["establishment_id", "application_type_id"],
    ),
  ],
)
data class EstablishmentApplicationType(

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  val id: UUID? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
    name = "establishment_id",
    nullable = false,
    foreignKey = ForeignKey(name = "fk_est_app_type_establishment"),
  )
  val establishment: Establishment,

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(
    name = "application_type_id",
    nullable = false,
    foreignKey = ForeignKey(name = "fk_est_app_type_app_type"),
  )
  val applicationType: ApplicationType,

  val displayOrder: Int = 0,
  val active: Boolean = true,
  val createdDate: LocalDateTime? = null,
  val lastModifiedDate: LocalDateTime? = null,
  val createdBy: String? = null,
  val lastModifiedBy: String? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as EstablishmentApplicationType
    return id != null && id == other.id
  }

  override fun hashCode(): Int = id?.hashCode() ?: javaClass.hashCode()
}
