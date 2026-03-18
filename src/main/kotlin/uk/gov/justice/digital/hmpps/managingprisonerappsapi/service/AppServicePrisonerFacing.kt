package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppListPrisonerFacing
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationTypeRepository

@Service
class AppServicePrisonerFacing(
  val appRepository: AppRepository,
  val applicationTypeRepository: ApplicationTypeRepository,
) {

  fun getAppsByPrisonerId(prisonerId: String): List<AppListPrisonerFacing> {
    val apps = appRepository.findAppsByRequestedBy(prisonerId)
    return convertAppsToAppResponsePrisonerFacing(apps)
  }

  fun convertAppsToAppResponsePrisonerFacing(apps: List<App>): List<AppListPrisonerFacing> {
    val appList = listOf<AppListPrisonerFacing>()
    apps.forEach { app ->
      appList.plus(convertAppToAppListPrisonerFacing(app))
    }
    return appList
  }

  fun convertAppToAppListPrisonerFacing(app: App): AppListPrisonerFacing {
    val applicationType = applicationTypeRepository.findById(app.applicationType).orElseThrow {
      throw ApiException("Application type with id: ${app.applicationType} not found", HttpStatus.INTERNAL_SERVER_ERROR)
    }
    return AppListPrisonerFacing(
      app.id,
      app.requestedBy,
      applicationType.name,
      app.createdDate,
      app.lastModifiedDate,
      app.status,
    )
  }
}
