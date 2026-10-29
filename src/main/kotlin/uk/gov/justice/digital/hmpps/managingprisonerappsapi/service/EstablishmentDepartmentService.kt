package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AssignedGroupDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Department
import java.util.UUID

interface EstablishmentDepartmentService {

  fun findDepartmentsForEstablishment(establishmentId: String): List<Department>

  fun findActiveDepartmentsForEstablishment(loggedUserId: String): List<AssignedGroupDto>

  fun findBySlugIgnoreCase(name: String?): Department

  fun findDepartmentByDepartmentIdAndEstablishmentId(departmentId: UUID, establishmentId: String): Department

  fun findDepartmentByDepartmentId(departmentId: UUID): Department
}
