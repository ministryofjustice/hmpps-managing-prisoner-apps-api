package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.EstablishmentApplicationTypeRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.ApplicationGroupResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentApplicationTypeResponseDto

interface EstablishmentApplicationTypeService {

  fun getAllApplicationTypesByEstablishment(staffId: String): List<EstablishmentApplicationTypeResponseDto>

  fun getActiveApplicationTypesByStaffId(staffId: String): List<ApplicationGroupResponse>

  fun getActiveApplicationTypesByPrisonerId(prisonerId: String): List<ApplicationGroupResponse>

  fun saveEstablishmentApplicationTypes(staffId: String, establishmentApplicationTypeRequestDtoList: List<EstablishmentApplicationTypeRequestDto>): List<EstablishmentApplicationTypeResponseDto>
}
