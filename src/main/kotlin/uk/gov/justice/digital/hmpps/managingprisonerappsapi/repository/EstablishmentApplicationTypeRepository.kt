package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EstablishmentApplicationType
import java.util.UUID

@Repository
interface EstablishmentApplicationTypeRepository : JpaRepository<EstablishmentApplicationType, UUID> {

  fun findByEstablishmentId(establishmentId: String, sort: Sort): List<EstablishmentApplicationType>

  fun findByEstablishmentIdAndActive(establishmentId: String, active: Boolean, sort: Sort): List<EstablishmentApplicationType>
}
