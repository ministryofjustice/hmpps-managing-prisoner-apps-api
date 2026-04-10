package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import com.fasterxml.uuid.Generators
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppRequestPrisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppListPrisonerFacing
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppResponsePrisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppResponsePrisonerFacing
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.ApplicationGroupResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.ApplicationTypeResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationGroup
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.SubmittedByType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationTypeRepository
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class AppServicePrisonerFacing(
  val appRepository: AppRepository,
  val applicationTypeRepository: ApplicationTypeRepository,
  val prisonerService: PrisonerService,
  val groupService: GroupService,
) {

  fun getAppsByPrisonerId(prisonerId: String): List<AppListPrisonerFacing> {
    val apps = appRepository.findAppsByRequestedBy(prisonerId)
    return convertAppsToAppResponsePrisonerFacing(apps)
  }

  fun getPrisonerAppById(prisonerId: String, appId: UUID): AppResponsePrisoner<Any, Any> {
    val prisoner = validatePrisoner(prisonerId)
    val app = appRepository.findById(appId).orElseThrow {
      ApiException("Prisoner with id $appId not found", HttpStatus.NOT_FOUND)
    }
    val group = groupService.getGroupsByLoggedStaffEstablishmentId(app.establishmentId)
    val applicationType = applicationTypeRepository.findById(app.applicationType!!)
      .orElseThrow { ApiException("No application type found for id: ${app.applicationType}", HttpStatus.BAD_REQUEST) }
    return convertAppEntityToAppResponse(app, prisoner, group, applicationType.applicationGroup!!, applicationType)
  }

  fun submitApp(appRequest: AppRequestPrisoner, prisonerId: String): AppResponsePrisoner<Any, Any> {
    val prisoner = validatePrisoner(prisonerId)
    val groups = groupService.getGroupByInitialAppType(prisoner.establishmentId!!, appRequest.applicationType!!)
    if (groups.isEmpty()) {
      throw ApiException("No department found to assigned app request", HttpStatus.BAD_REQUEST)
    }
    val applicationType = applicationTypeRepository.findById(appRequest.applicationType!!)
      .orElseThrow { ApiException("No application type found for $appRequest", HttpStatus.BAD_REQUEST) }
    val app = convertAppRequestToAppRequestEntity(appRequest, prisonerId, prisoner, groups[0].id, applicationType.applicationGroup!!.id, applicationType.id)
    val appEntity = appRepository.save(app)
    return convertAppEntityToAppResponse(appEntity, prisoner, groups[0].id, applicationType.applicationGroup!!, applicationType)
  }

  private fun convertAppRequestToAppRequestEntity(appRequest: AppRequestPrisoner, prisonerId: String, prisoner: Prisoner, groupId: UUID, applicationType: Long, applicationGroup: Long): App {
    val localDateTime = LocalDateTime.now(ZoneOffset.UTC)
    var firstNightCenter = false
    return App(
      Generators.timeBasedEpochGenerator().generate(), // id
      appRequest.reference, // reference
      groupId, // group id or department
      null,
      applicationGroup,
      applicationType,
      appRequest.genericForm,
      localDateTime, // last modified date
      localDateTime, // created by
      prisonerId,
      SubmittedByType.PRISONER,
      localDateTime,
      prisonerId,
      mutableListOf(),
      convertRequestsToAppRequests(appRequest.requests),
      prisoner.username,
      prisoner.firstName,
      prisoner.lastName,
      AppStatus.PENDING,
      prisoner.establishmentId!!,
      mutableListOf(),
      firstNightCenter,
    )
  }

  private fun convertAppEntityToAppResponse(
    app: App,
    prisoner: Any,
    assignedGroup: Any,
    applicationGroup: ApplicationGroup,
    applicationType: ApplicationType,
  ): AppResponsePrisoner<Any, Any> = AppResponsePrisoner(
    app.id,
    app.reference,
    assignedGroup,
    ApplicationTypeResponse(applicationType.id, applicationType.name, null, null, null, null),
    app.genericForm,
    ApplicationGroupResponse(applicationGroup.id, applicationGroup.name, null),
    app.requestedDate,
    app.createdDate,
    app.createdBy,
    app.lastModifiedDate,
    app.lastModifiedBy,
    app.requests,
    prisoner,
    app.requestedByFirstName,
    app.requestedByLastName,
    app.status,
    app.establishmentId,
  )

  private fun convertRequestsToAppRequests(requests: List<Map<String, Any>>): List<MutableMap<String, Any>> {
    val appRequests = ArrayList<MutableMap<String, Any>>()
    requests.forEach { request ->
      val map = HashMap<String, Any>()
      map.put("id", UUID.randomUUID().toString())
      request.keys.forEach { key ->
        request.get(key)?.let { map.put(key, it) }
      }
      appRequests.add(map)
    }
    return appRequests
  }

  private fun convertAppToAppResponsePrisonerFacing(
    app: App,
    prisoner: Any,
    assignedGroup: Any,
    applicationGroup: ApplicationGroup,
    applicationType: ApplicationType,
  ): AppResponsePrisonerFacing<Any, Any> = AppResponsePrisonerFacing<Any, Any>(
    app.id,
    app.reference,
    assignedGroup,
    ApplicationTypeResponse(applicationType.id, applicationType.name, null, null, null, null),
    app.genericForm,
    ApplicationGroupResponse(applicationGroup.id, applicationGroup.name, null),
    app.requestedDate,
    app.createdDate,
    app.createdBy,
    app.lastModifiedDate,
    app.requests,
    prisoner,
    app.requestedByFirstName,
    app.requestedByLastName,
    app.status,
    app.establishmentId,
  )

  private fun convertAppsToAppResponsePrisonerFacing(apps: List<App>): List<AppListPrisonerFacing> {
    val appList = listOf<AppListPrisonerFacing>()
    apps.forEach { app ->
      appList.plus(convertAppToAppListPrisonerFacing(app))
    }
    return appList
  }

  private fun convertAppToAppListPrisonerFacing(app: App): AppListPrisonerFacing {
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

  private fun validatePrisoner(prisonerId: String): Prisoner {
    val prisoner = prisonerService.getPrisonerById(prisonerId).orElseThrow {
      ApiException("Prison with id $prisonerId not found", HttpStatus.NOT_FOUND)
    }
    return prisoner
  }
}
