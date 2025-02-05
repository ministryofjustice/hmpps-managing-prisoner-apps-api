package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppDto
import java.util.UUID

interface AppService {
  fun submitApp(appDto: AppDto): AppDto

  fun getAppsById(id: UUID): AppDto

  fun getAppsByEstablishment(name: String): AppDto
}