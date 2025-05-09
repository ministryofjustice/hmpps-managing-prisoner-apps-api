package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import java.util.*

data class Staff(
  val username: String,
  val userId: String,
  val fullName: String,
  val category: UserCategory,
  val establishmentId: String,
  // val roles: Set<UUID>,
  val jobTitle: String,
  val uuid: UUID?,
)
