package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import java.util.*

data class Staff(
  val username: String,
  val userId: Long?,
  val name: String?,
  val category: UserCategory? = null,
  val roles: Set<UUID>? = null,
  val jobTitle: String? = null,
  val uuid: UUID?,
)
