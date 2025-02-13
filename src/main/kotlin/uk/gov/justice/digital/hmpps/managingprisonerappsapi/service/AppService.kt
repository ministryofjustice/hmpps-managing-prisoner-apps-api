package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppResponseDto
import java.util.UUID

interface AppService {
  fun submitApp(prisonerId : String, staffId: String, appRequestDto: AppRequestDto): AppResponseDto

  fun getAppsById(id: UUID): AppResponseDto

  fun getAppsByEstablishment(name: String): AppResponseDto
}