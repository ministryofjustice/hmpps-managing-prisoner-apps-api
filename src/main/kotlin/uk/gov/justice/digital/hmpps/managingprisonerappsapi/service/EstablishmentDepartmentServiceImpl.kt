package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AssignedGroupDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Department
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EstablishmentDepartment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.DepartmentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentDepartmentRepository
import java.util.UUID

@Service
class EstablishmentDepartmentServiceImpl(
  private val establishmentDepartmentRepository: EstablishmentDepartmentRepository,
  private val departmentRepository: DepartmentRepository,
  private val staffService: StaffService,
  private val establishmentService: EstablishmentService,

) : EstablishmentDepartmentService {

  @Transactional(readOnly = true)
  override fun findDepartmentsForEstablishment(establishmentId: String): List<Department> = establishmentDepartmentRepository.findByEstablishmentId(establishmentId).map { it.department }

  @Transactional(readOnly = true)
  override fun findActiveDepartmentsForEstablishment(loggedUserId: String): List<AssignedGroupDto> {
    val staff = staffService.getStaffById(loggedUserId).orElseThrow {
      ApiException("Staff with id $loggedUserId not found", HttpStatus.NOT_FOUND)
    }
    establishmentService.getEstablishmentById(staff.establishmentId).orElseThrow {
      ApiException("Establishment ${staff.establishmentId} not onboarded yet", HttpStatus.FORBIDDEN)
    }
    val establishmentDepartments = establishmentDepartmentRepository.findByEstablishmentIdAndActive(staff.establishmentId, true)
    return convertDepartmentsToAssignedGroupsDto(establishmentDepartments)
  }

  // override fun findDepartmentByGroupId(groupId: UUID): Department = departmentRepository.findDepartmentByGroupId(groupId)

  override fun findBySlugIgnoreCase(name: String?): Department = departmentRepository.findBySlugIgnoreCase(name)

  override fun findDepartmentByDepartmentIdAndEstablishmentId(departmentId: UUID, establishmentId: String): Department {
    val establishmentDepartment =
      establishmentDepartmentRepository.findByEstablishmentIdAndDepartmentId(establishmentId, departmentId)
    return establishmentDepartment?.department
      ?: throw ApiException("Department $departmentId not found for establishment $establishmentId", HttpStatus.NOT_FOUND)
  }

  override fun findDepartmentByDepartmentId(departmentId: UUID): Department = departmentRepository.findById(departmentId).orElseThrow {
    ApiException("Department with id $departmentId not found", HttpStatus.NOT_FOUND)
  }

  private fun convertDepartmentsToAssignedGroupsDto(departments: List<EstablishmentDepartment>): List<AssignedGroupDto> {
    val assignedGroupDtoList = mutableListOf<AssignedGroupDto>()
    departments.forEach { it ->
      assignedGroupDtoList.add(
        AssignedGroupDto(
          id = it.department.slug,
          name = it.department.name,
          establishment = convertToEstablishmentDto(it.establishment),
          initialApp = null,
          type = GroupType.WING,
        ),
      )
    }
    return assignedGroupDtoList
  }

  private fun convertToEstablishmentDto(establishment: Establishment): EstablishmentDto = EstablishmentDto(
    id = establishment.id,
    name = establishment.name,
    defaultDepartments = false,
  )
}
