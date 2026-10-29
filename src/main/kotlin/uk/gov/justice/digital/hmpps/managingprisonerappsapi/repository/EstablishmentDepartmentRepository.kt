package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EstablishmentDepartment
import java.util.UUID

@Repository
interface EstablishmentDepartmentRepository : JpaRepository<EstablishmentDepartment, UUID> {

  fun findByEstablishmentId(establishmentId: String): List<EstablishmentDepartment>

  fun findByEstablishmentIdAndDepartmentId(establishmentId: String, departmentId: UUID): EstablishmentDepartment

  fun findByEstablishmentIdAndActive(establishmentId: String, active: Boolean): List<EstablishmentDepartment>
}
