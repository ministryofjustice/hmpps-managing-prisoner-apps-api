package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppDecisionRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppDecisionResponseDto
import java.util.*

interface ResponseService {

  fun addResponse(prisonerId: String, appId: UUID, staffId: String, response: AppDecisionRequestDto): AppDecisionResponseDto<Any>

  fun getResponseById(prisonerId: String, appId: UUID, staffId: String, createdBy: Boolean, responseId: UUID): AppDecisionResponseDto<Any>
}
