package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "establishment_application_type")
data class EstablishmentApplicationType(

  @EmbeddedId
  val id: EstablishmentApplicationTypeId,

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("establishmentId")
  val establishment: Establishment,

  @ManyToOne(fetch = FetchType.EAGER)
  @MapsId("applicationTypeId")
  val applicationType: ApplicationType,

  /**
   Reference to the parent establishment_application_group.
   * This enforces that a type can only exist if its parent group is configured for the establishment.
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumns(
    JoinColumn(name = "establishment_id", referencedColumnName = "establishment_id", insertable = false, updatable = false),
    JoinColumn(name = "application_group_id", referencedColumnName = "application_group_id", insertable = false, updatable = false),
  )
  val establishmentApplicationGroup: EstablishmentApplicationGroup,

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
    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()
}
