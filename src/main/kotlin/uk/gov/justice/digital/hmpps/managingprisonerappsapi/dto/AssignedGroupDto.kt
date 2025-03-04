package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType

data class AssignedGroupDto(
  val name: String?,
  val establishment: EstablishmentDto,
  val initialApp: AppType?,
  val type: GroupType?,
  val email: String?,
)

data class EstablishmentDto(
  val id: String,
  val name: String,
)
