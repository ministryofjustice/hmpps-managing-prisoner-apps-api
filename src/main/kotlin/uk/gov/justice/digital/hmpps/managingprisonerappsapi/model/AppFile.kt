package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import java.time.LocalDateTime
import java.util.UUID

@Entity
data class AppFile(
  @Id
  val id: UUID,
  val documentId: String,
  val fileName: String,
  val createdDate: LocalDateTime,
  val createdBy: String,
  val fileType: String,
  @ManyToOne(fetch = FetchType.EAGER)
  @JsonIgnore
  var app: App? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AppFile

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()
}
