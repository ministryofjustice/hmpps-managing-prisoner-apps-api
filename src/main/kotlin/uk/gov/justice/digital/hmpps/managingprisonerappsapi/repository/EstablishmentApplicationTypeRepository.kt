package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EstablishmentApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EstablishmentApplicationTypeId

@Repository
interface EstablishmentApplicationTypeRepository : JpaRepository<EstablishmentApplicationType, EstablishmentApplicationTypeId> {

  fun findByIdEstablishmentIdOrderByDisplayOrder(establishmentId: String): List<EstablishmentApplicationType>

  fun findByIdEstablishmentIdAndActiveOrderByDisplayOrder(establishmentId: String, active: Boolean): List<EstablishmentApplicationType>

  fun findByIdEstablishmentIdAndIdApplicationGroupIdAndActiveOrderByDisplayOrder(
    establishmentId: String,
    applicationGroupId: Long,
    active: Boolean,
  ): List<EstablishmentApplicationType>
}
