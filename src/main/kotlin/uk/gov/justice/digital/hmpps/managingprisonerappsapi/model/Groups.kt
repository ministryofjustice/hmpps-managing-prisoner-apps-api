package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "groups")
data class Groups(
  @Id
  val id: UUID,
  val name: String,
  val establishmentId: String,
  // @JdbcTypeCode(SqlTypes.JSON)
  @ElementCollection
  @Enumerated(EnumType.STRING)
  val initialsApps: List<AppType>,
  @Enumerated(EnumType.STRING)
  val type: GroupType,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Groups

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()
}
