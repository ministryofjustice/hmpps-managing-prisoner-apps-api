package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Department
import java.util.UUID

@Repository
interface DepartmentRepository : JpaRepository<Department, UUID> {

  @Query(
    value = """
        SELECT d.* FROM groups g 
        INNER JOIN department d  ON lower(trim(d.name)) = lower(trim(g.name)) 
        INNER JOIN establishment_departments ed ON ed.department_id = d.id     
        AND ed.establishment_id = g.establishment_id WHERE g.id = :groupId AND ed.active = true  
        """,
    nativeQuery = true,
  )
  fun findBySlugIgnoreCase(name: String?): Department
}
