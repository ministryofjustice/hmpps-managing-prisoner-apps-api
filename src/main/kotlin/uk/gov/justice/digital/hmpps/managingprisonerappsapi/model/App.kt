package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "app")
data class App(
  @Id
  val id: UUID,
  val reference: String?,
  var assignedGroup: UUID,
  @Enumerated(EnumType.STRING)
  val appType: AppType,
  val requestedDate: LocalDateTime,
  val createdDate: LocalDateTime,
  val createdBy: String,
  val lastModifiedDate: LocalDateTime?,
  val lastModifiedBy: String?,
  @ElementCollection
  var comments: MutableList<UUID>,
  @JdbcTypeCode(SqlTypes.JSON)
  var requests: List<Map<String, Any>>,
  val requestedBy: String,
  val requestedByFirstName: String,
  val requestedByLastName: String,
  @Enumerated(EnumType.STRING)
  var status: AppStatus,
  val establishmentId: String,
  @ElementCollection
  var responses: MutableList<UUID>,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as App

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()
}
