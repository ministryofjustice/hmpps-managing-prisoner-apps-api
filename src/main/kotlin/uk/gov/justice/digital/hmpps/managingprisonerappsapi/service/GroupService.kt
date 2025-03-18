package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AssignedGroupDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.GroupsRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Groups
import java.util.UUID

interface GroupService {

  fun createGroup(groupRequestDto: GroupsRequestDto): AssignedGroupDto

  fun updateGroup(groupRequestDto: GroupsRequestDto): AssignedGroupDto

  fun getGroupsByEstablishment(establishmentId: UUID, name: String)

  fun getGroupById(id: UUID): AssignedGroupDto

  fun deleteGroupById(id: UUID)

  fun getGroupsByEstablishmentId(id: String): List<AssignedGroupDto>

  fun getGroupByInitialAppType(appType: AppType): Groups

  fun convertGroupsToAssignedGroupsDto(groups: Groups, establishment: Establishment): AssignedGroupDto
}
