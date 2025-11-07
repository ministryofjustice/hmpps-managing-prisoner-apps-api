package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import java.util.UUID

data class AssignedGroupDto(
  val id: UUID,
  val name: String,
  val establishment: EstablishmentDto,
  val initialApp: Long?,
  val type: GroupType?,
)

data class EstablishmentDto(
  val id: String,
  val name: String,
  val appTypes: Set<AppType>,
  val defaultDepartments: Boolean,
  val blacklistedAppGroups: Set<Long>,
  val blacklistedAppTypes: Set<Long>
)
