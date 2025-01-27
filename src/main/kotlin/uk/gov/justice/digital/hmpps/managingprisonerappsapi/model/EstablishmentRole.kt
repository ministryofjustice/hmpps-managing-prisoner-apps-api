package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import java.util.*

data class EstablishmentRole(
  val id: UUID,
  val establishment: UUID,
  val role: Role
)
