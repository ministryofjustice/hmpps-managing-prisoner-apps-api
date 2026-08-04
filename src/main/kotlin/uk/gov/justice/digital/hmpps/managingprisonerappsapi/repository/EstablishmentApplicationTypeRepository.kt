package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EstablishmentApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EstablishmentApplicationTypeId

@Repository
interface EstablishmentApplicationTypeRepository : JpaRepository<EstablishmentApplicationType, EstablishmentApplicationTypeId> {

  fun findByEstablishmentIdOrderByDisplayOrder(establishmentId: String): List<EstablishmentApplicationType>

  fun findByEstablishmentIdAndActiveOrderByDisplayOrder(establishmentId: String, active: Boolean): List<EstablishmentApplicationType>

/*  @Query(
    """
        SELECT eat FROM EstablishmentApplicationType eat
        WHERE eat.establishmentId = :establishmentId AND eat.active = :active
        AND eat.applicationTypeId IN ( SELECT at.id FROM ApplicationType at
            WHERE at.applicationGroup.id = :groupId
        )
        ORDER BY eat.displayOrder
    """,
  )
  fun findByEstablishmentIdAndGroupIdAndActiveOrderByDisplayOrder(
    establishmentId: String,
    groupId: Long,
    active: Boolean,
  ): List<EstablishmentApplicationType>*/

/*  @Query(
    """
        SELECT COUNT(eat) FROM EstablishmentApplicationType eat
        WHERE eat.establishmentId = :establishmentId
        AND eat.active = true AND eat.applicationTypeId IN (
            SELECT at.id FROM ApplicationType at
            WHERE at.applicationGroup.id = :groupId
        )
    """,
  )
  fun countActiveTypesByEstablishmentAndGroup(establishmentId: String, groupId: Long): Long*/
}
