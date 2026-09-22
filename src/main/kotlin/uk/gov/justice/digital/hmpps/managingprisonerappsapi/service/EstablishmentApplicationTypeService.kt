package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.ApplicationGroupResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentApplicationTypeResponse

interface EstablishmentApplicationTypeService {

  fun getAllApplicationTypesByEstablishment(staffId: String): List<EstablishmentApplicationTypeResponse>

  fun getActiveApplicationTypesByStaffId(staffId: String): List<ApplicationGroupResponse>

  fun getActiveApplicationTypesByPrisonerId(prisonerId: String): List<ApplicationGroupResponse>
}
