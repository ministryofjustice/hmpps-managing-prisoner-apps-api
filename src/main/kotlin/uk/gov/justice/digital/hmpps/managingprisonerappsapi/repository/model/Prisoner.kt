package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.model

import java.util.UUID

data class Prisoner (
  val id: UUID,
  val firstName: String,
  val lastName: String,
  val category: UserCategory,
  val location : String
)
