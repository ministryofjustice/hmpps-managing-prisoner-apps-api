package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.UUID

@Entity
data class Group(
  @Id
  val id: UUID,
  val name: String,
  val establishment: UUID,
  @ElementCollection
  val staffs: Set<UUID>,
  @ElementCollection
  val initialsApps: List<AppType>,
  val type: GroupType
  )
