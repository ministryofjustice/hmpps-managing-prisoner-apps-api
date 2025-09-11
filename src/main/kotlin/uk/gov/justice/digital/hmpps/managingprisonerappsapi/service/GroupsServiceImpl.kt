package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import com.fasterxml.uuid.Generators
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.GroupsRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AssignedGroupDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Groups
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.GroupRepository
import java.util.*
import kotlin.collections.ArrayList

@Service
class GroupsServiceImpl(
  private var groupRepository: GroupRepository,
  private val establishmentService: EstablishmentService,
  private val staffService: StaffService,
) : GroupService {
  override fun createGroup(groupRequestDto: GroupsRequestDto): AssignedGroupDto {
    val establishment = establishmentService.getEstablishmentById(groupRequestDto.establishmentId).orElseThrow {
      ApiException("Establishment with id ${groupRequestDto.establishmentId} not found", HttpStatus.BAD_REQUEST)
    }
    var groups = convertGroupsRequestDtoToGroups(groupRequestDto)
    groups = groupRepository.save(groups)
    return convertGroupsToAssignedGroupsDto(groups, establishment)
  }

  override fun updateGroup(groupRequestDto: GroupsRequestDto): AssignedGroupDto {
    val establishment = establishmentService.getEstablishmentById(groupRequestDto.establishmentId).orElseThrow {
      ApiException("Establishment with id ${groupRequestDto.establishmentId} not found", HttpStatus.BAD_REQUEST)
    }
    groupRepository.findById(groupRequestDto.id).orElseThrow {
      ApiException("Group with id ${groupRequestDto.id} not found", HttpStatus.NOT_FOUND)
    }
    var groups = convertGroupsRequestDtoToGroups(groupRequestDto)
    groups = groupRepository.save(groups)
    return convertGroupsToAssignedGroupsDto(groups, establishment)
  }

  override fun getGroupsByEstablishment(establishmentId: UUID, name: String) {
    TODO("Not yet implemented")
  }

  override fun getGroupById(id: UUID): AssignedGroupDto {
    val groups = groupRepository.findById(id).orElseThrow {
      ApiException("Group with id $id not found", HttpStatus.NOT_FOUND)
    }
    val establishment = establishmentService.getEstablishmentById(groups.establishmentId).orElseThrow {
      ApiException("Establishment with id ${groups.establishmentId} not found", HttpStatus.NOT_FOUND)
    }
    return convertGroupsToAssignedGroupsDto(groups, establishment)
  }

  override fun deleteGroupById(id: UUID) {
    groupRepository.deleteById(id)
  }

  override fun getGroupsByEstablishmentId(establishmentId: String): List<AssignedGroupDto> = findGroupsByEstablishmentId(establishmentId)

  override fun getGroupsByLoggedStaffEstablishmentId(loggedUserId: String): List<AssignedGroupDto> {
    val staff = staffService.getStaffById(loggedUserId).orElseThrow {
      ApiException("Staff with id $loggedUserId not found", HttpStatus.NOT_FOUND)
    }
    val establishmentDto = establishmentService.getEstablishmentById(staff.establishmentId).orElseThrow {
      ApiException("Establishment ${staff.establishmentId} not onboarded yet", HttpStatus.FORBIDDEN)
    }
    val establishmentId = if (establishmentDto.defaultDepartments)  "DEFAULT" else staff.establishmentId
    return findGroupsByEstablishmentId(establishmentId)
  }

  override fun getGroupsByLoggedStaffEstablishmentIdAndAppType(loggedUserId: String, appType: AppType): List<AssignedGroupDto> {
    val staff = staffService.getStaffById(loggedUserId).orElseThrow {
      ApiException("Staff with id $loggedUserId not found", HttpStatus.NOT_FOUND)
    }
    val establishmentDto = establishmentService.getEstablishmentById(staff.establishmentId).orElseThrow {
      ApiException("Establishment ${staff.establishmentId} not onboarded yet", HttpStatus.FORBIDDEN)
    }
    val establishmentId = if (establishmentDto.defaultDepartments)  "DEFAULT" else staff.establishmentId
    val groups = groupRepository.getGroupsByEstablishmentIdOrderByName(establishmentId)
    val groupList: MutableList<Groups> = mutableListOf()
    for (group in groups) {
      if (group.initialsApps.contains(appType)) {
        groupList.add(group)
      }
    }
    if (groupList.size <= 1) {
      if (groupList.size == 1) {
        groupList.removeLast()
      }
      groupList.addAll(groups)
    }
    val departments = ArrayList<AssignedGroupDto>()
    for (g in groupList) {
      departments.add(convertGroupsToAssignedGroupsDto(g, establishmentDto))
    }
    return departments
  }

  override fun convertGroupsToAssignedGroupsDto(
    groups: Groups,
    establishmentDto: EstablishmentDto,
  ): AssignedGroupDto = AssignedGroupDto(
    groups.id,
    groups.name,
    establishmentDto,
    if (groups.initialsApps.isNotEmpty()) groups.initialsApps.first() else null,
    groups.type,
  )

  fun convertGroupsRequestDtoToGroups(groupsRequestDto: GroupsRequestDto): Groups = Groups(
    Generators.timeBasedEpochGenerator().generate(),
    groupsRequestDto.name,
    groupsRequestDto.establishmentId,
    groupsRequestDto.initialsApps,
    groupsRequestDto.type,
  )

  override fun getGroupByInitialAppType(establishmentId: String, appType: AppType): List<Groups> {
    val groupList = groupRepository.findGroupsByEstablishmentIdAndInitialsAppsIsContaining(establishmentId, appType)
    /*if (groupList.isEmpty()) {
      throw ApiException("Groups list is empty", HttpStatus.BAD_REQUEST)
    } else {
      return groupList
    }*/
    return groupList
  }

  private fun findGroupsByEstablishmentId(establishmentId: String): List<AssignedGroupDto> {
    val groups = groupRepository.getGroupsByEstablishmentIdOrderByName(establishmentId)
    val assignedGroupDto = ArrayList<AssignedGroupDto>()
    groups.forEach { g ->
      val establishment = establishmentService.getEstablishmentById(g.establishmentId)
        .orElseThrow { ApiException("Establishment with ${g.id} not found", HttpStatus.NOT_FOUND) }
      assignedGroupDto.add(convertGroupsToAssignedGroupsDto(g, establishment))
    }
    return assignedGroupDto
  }
}
