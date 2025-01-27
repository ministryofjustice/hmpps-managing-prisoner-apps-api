package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.UUID

@Entity
data class Prisoner (
  @Id
  val id: UUID,
  val firstName: String,
  val lastName: String,
  val category: UserCategory,
  val location : String
)
