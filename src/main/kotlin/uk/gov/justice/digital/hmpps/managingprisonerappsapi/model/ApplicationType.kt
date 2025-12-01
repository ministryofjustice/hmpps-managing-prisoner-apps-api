package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OrderBy

@Entity
data class ApplicationType(
  @Id
  val id: Long,
  @OrderBy("name")
  val name: String,

  val genericType: Boolean,
  val genericForm: Boolean = false,
  val logDetailRequired: Boolean,
) {

  @ManyToOne(fetch = FetchType.EAGER)
  @JsonIgnore
  var applicationGroup: ApplicationGroup? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Long

    return id == other
  }

  override fun hashCode(): Int = id.hashCode()
}
