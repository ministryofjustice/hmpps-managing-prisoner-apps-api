package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Department
import java.util.UUID

@Repository
interface DepartmentRepository : JpaRepository<Department, UUID> {

  fun findBySlugIgnoreCase(departmentSlug: String?): Department?
}
