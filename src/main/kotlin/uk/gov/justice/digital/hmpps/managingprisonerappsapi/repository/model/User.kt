package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.model

import java.util.UUID

abstract  class User(
  val id: UUID,
  val firstName: String,
  val lastName: String,
  val category: UserCategory
)
