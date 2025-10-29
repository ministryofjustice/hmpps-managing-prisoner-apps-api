package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany

@Entity
data class ApplicationGroup(
  @Id
  val id: Long,

  val name: String,

  @OneToMany(mappedBy = "applicationGroup", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  var applicationTypes: List<ApplicationType> = listOf<ApplicationType>(),
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Long

    return id == other
  }

  override fun hashCode(): Int = id.hashCode()
}
