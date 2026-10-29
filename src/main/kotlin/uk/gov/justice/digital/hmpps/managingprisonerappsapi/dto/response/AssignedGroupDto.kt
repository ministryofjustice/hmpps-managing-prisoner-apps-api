package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType

data class AssignedGroupDto(
  val id: String,
  val name: String,
  val establishment: EstablishmentDto,
  val initialApp: Long?,
  val type: GroupType?,
)

data class EstablishmentDto(
  val id: String,
  val name: String,
  val defaultDepartments: Boolean,
)
