package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.model

import jakarta.persistence.Id
import java.util.UUID

abstract  class User(
  @Id
  val id: UUID,
  val firstName: String,
  val lastName: String,
  val category: UserCategory
)
