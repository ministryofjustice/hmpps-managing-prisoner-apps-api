package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EstablishmentApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EstablishmentApplicationTypeView
import java.util.UUID

@Repository
interface EstablishmentApplicationTypeRepository : JpaRepository<EstablishmentApplicationType, UUID> {

  fun findByEstablishmentId(establishmentId: String, sort: Sort): List<EstablishmentApplicationType>

  fun findByEstablishmentIdAndActive(establishmentId: String, active: Boolean, sort: Sort): List<EstablishmentApplicationType>

  @Query(
    """
    SELECT eat FROM EstablishmentApplicationType eat
    WHERE eat.establishment.id = :establishmentId
      AND eat.active = true
      AND eat.departmentSlug IS NOT NULL
      AND trim(eat.departmentSlug) <> ''
    """,
  )
  fun findActiveTypesWithDepartmentForEstablishment(establishmentId: String, sort: Sort): List<EstablishmentApplicationType>

  @Query(
    """
    SELECT at.id as applicationTypeId, ag.id as applicationGroupId, eat.active AS active
    FROM EstablishmentApplicationType eat
    JOIN eat.applicationType at
    JOIN at.applicationGroup ag
    WHERE eat.establishment.id = :establishmentId and at.id = :applicationTypeId
    """,
  )
  fun findApplicationTypeForEstablishment(establishmentId: String, applicationTypeId: Long): EstablishmentApplicationTypeView

  fun findByEstablishmentIdAndApplicationTypeId(establishmentId: String, applicationTypeId: Long): EstablishmentApplicationType
}
