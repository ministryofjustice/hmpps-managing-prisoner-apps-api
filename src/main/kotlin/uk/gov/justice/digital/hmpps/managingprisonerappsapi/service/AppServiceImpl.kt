package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppDto
import java.util.*

class AppServiceImpl : AppService {
  override fun submitApp(appDto: AppDto): AppDto {
    TODO("Not yet implemented")
  }

  override fun getAppsById(id: UUID): AppDto {
    TODO("Not yet implemented")
  }

  override fun getAppsByEstablishment(name: String): AppDto {
    TODO("Not yet implemented")
  }
}