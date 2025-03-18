package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.UUID

@Entity
data class App(
  @Id
  val id: UUID,
  val reference: String,
  var assignedGroup: UUID,
  val appType: AppType,
  val requestedDate: LocalDateTime,
  val createdDate: LocalDateTime,
  val createdBy: String,
  val lastModifiedDate: LocalDateTime?,
  val lastModifiedBy: String?,
  @ElementCollection
  val comments: List<UUID>,
  @JdbcTypeCode(SqlTypes.JSON)
  val requests: List<Map<String, Any>>,
  val requestedBy: String,
  val status: AppStatus,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as App

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()
}
