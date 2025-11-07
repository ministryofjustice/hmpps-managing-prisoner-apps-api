package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import java.util.*

data class GroupsRequestDto(
  val id: UUID,
  val name: String,
  val establishmentId: String,
  val initialsApps: List<Long>,
  val type: GroupType,
)
