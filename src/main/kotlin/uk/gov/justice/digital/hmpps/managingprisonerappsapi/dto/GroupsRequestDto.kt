package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto

import jakarta.persistence.ElementCollection
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import java.util.*

data class GroupsRequestDto(
  val id: UUID,
  val name: String,
  val establishmentId: String,
  val initialsApps: List<AppType>,
  val type: GroupType,
)
