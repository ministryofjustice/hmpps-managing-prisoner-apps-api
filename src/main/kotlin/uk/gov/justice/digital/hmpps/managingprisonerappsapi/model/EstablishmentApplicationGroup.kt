package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import java.time.LocalDateTime

@Entity
data class EstablishmentApplicationGroup(
  @EmbeddedId
  val id: EstablishmentApplicationGroupId,

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("establishmentId")
  val establishment: Establishment,

  @ManyToOne(fetch = FetchType.EAGER)
  @MapsId("applicationGroupId")
  val applicationGroup: ApplicationGroup,

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
    other as EstablishmentApplicationGroup
    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()
}
