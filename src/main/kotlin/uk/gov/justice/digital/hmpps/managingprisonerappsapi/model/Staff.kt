package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.*

@Entity
data class Staff(
  @Id
  val id: UUID,
  val firstName: String,
  val lastName: String,
  val category: UserCategory,
  @ElementCollection
  val roles: Set<UUID>,
  val jobTitle: String
)
