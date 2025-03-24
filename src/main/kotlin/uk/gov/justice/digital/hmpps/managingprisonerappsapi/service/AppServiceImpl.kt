package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppListViewDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppResponseListDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.GroupAppListViewDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppByAppTypeCounts
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.RequestedByNameSearchResult
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource.AppResource
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class AppServiceImpl(
  var appRepository: AppRepository,
  var prisonerService: PrisonerService,
  var staffService: StaffService,
  var groupsService: GroupService,
  var establishmentService: EstablishmentService,
) : AppService {

  companion object {
    private val logger = LoggerFactory.getLogger(AppResource::class.java)
  }

  fun saveApp(app: App): App = appRepository.save(app)

  fun updateApp(app: App): App = appRepository.save(app)

  fun getAppByID(id: UUID): Optional<App> = appRepository.findById(id)

  fun deleteAppById(id: UUID) {
    appRepository.deleteById(id)
  }

  fun getAppByRequestedBy(): List<App> {
    TODO("Not yet implemented")
  }

  fun getAppsByEstablishment() {
    TODO("Not yet implemented")
  }

  fun getAppsByGroup() {
  }

  override fun submitApp(prisonerId: String, staffId: String, appRequestDto: AppRequestDto): AppResponseDto {
    //   TODO("Not yet implemented")
    // validate prisoner
    val prisoner = prisonerService.getPrisonerById(prisonerId)
    val staff = staffService.getStaffById(staffId)
    var app = convertAppRequestToAppEntity(prisoner.get(), staff.get(), appRequestDto)
    app = appRepository.save(app)
    val assignedGroup = groupsService.getGroupByInitialAppType(staff.get().establishmentId, app.appType)
    logger.info("App created for $prisonerId for app type ${app.appType}")
    return convertAppToAppResponseDto(app, prisonerId, assignedGroup)
  }

  override fun getAppsById(prisonerId: String, id: UUID, requestedBy: Boolean, assignedGroup: Boolean): AppResponseDto {
    val app = appRepository.findAppsByIdAndRequestedBy(id, prisonerId)
      .orElseThrow<ApiException> { throw ApiException("No app exist with id $id", HttpStatus.NOT_FOUND) }

    val groups = groupsService.getGroupById(app.assignedGroup)
    val groupsDto: Any
    if (assignedGroup) {
      groupsDto = groups
    } else {
      groupsDto = groups.id
    }
    val prisoner: Any
    if (requestedBy) {
      prisoner = prisonerService.getPrisonerById(prisonerId)
        .orElseThrow { throw ApiException("No prisoner exist with id $prisonerId", HttpStatus.NOT_FOUND) }
    } else {
      prisoner = prisonerId
    }
    return convertAppToAppResponseDto(app, prisoner, groupsDto)
  }

  override fun getAppsByEstablishment(name: String): AppResponseDto {
    TODO("Not yet implemented")
  }

  override fun forwardAppToGroup(groupId: UUID, appId: UUID): AppResponseDto {
    val app = appRepository.findById(appId).orElseThrow { throw ApiException("No app found with id $appId", HttpStatus.NOT_FOUND) }
    app.assignedGroup = groupId
    appRepository.save(app)
    return convertAppToAppResponseDto(app, app.requestedBy, app.assignedGroup)
  }

  override fun searchAppsByColumnsFilter(
    staffId: String,
    status: Set<AppStatus>,
    appTypes: Set<AppType>?,
    requestedBy: String?,
    assignedGroups: Set<UUID>?,
    pageNumber: Long,
    pageSize: Long,
  ): AppResponseListDto {
    val staff = staffService.getStaffById(staffId).orElseThrow {
      throw ApiException("No staff with id $staffId", HttpStatus.BAD_REQUEST)
    }
    val appTypeDto = appRepository.countBySearchFilter(
      staff.establishmentId,
      status,
      appTypes,
      requestedBy,
      assignedGroups,
      )

    val pageRequest = PageRequest.of((pageNumber - 1).toInt(), pageSize.toInt())
    val pageResult = appRepository.appsBySearchFilter(
      staff.establishmentId,
      status,
      appTypes,
      requestedBy,
      assignedGroups,
      pageRequest
    )
    val appsList = convertAppToAppListDto(pageResult.content)
    return AppResponseListDto(
      pageResult.pageable.pageNumber + 1,
      pageResult.totalElements,
      pageResult.isLast,
      convertAppTypeCountsToMap(appTypeDto),
      appsList,
    )
  }

  override fun searchRequestedByTextSearch(staffId: String, text: String): List<RequestedByNameSearchResult> {
    if (text.isBlank() || text.length < 3) {
      throw ApiException("Text search cannot be empty or just whitespaces or less than 3 chars",  HttpStatus.BAD_REQUEST)
    }
    val staff = staffService.getStaffById(staffId).orElseThrow {  throw ApiException("No staff with id $staffId found", HttpStatus.NOT_FOUND) }
    return appRepository.searchRequestedByFullName(staff.establishmentId, text)
  }

  private fun convertAppRequestToAppEntity(prisoner: Prisoner, staff: Staff, appRequest: AppRequestDto): App {
    val localDateTime = LocalDateTime.now(ZoneOffset.UTC)
    return App(
      UUID.randomUUID(), // id
      appRequest.reference, // reference
      UUID.randomUUID(), // group id
      AppType.getAppType(appRequest.type),
      appRequest.requestedDate,
      localDateTime, // created date
      staff.username,
      localDateTime, // last modified date
      staff.username, // created by
      arrayListOf(),
      appRequest.requests,
      prisoner.username,
      "${prisoner.firstName} ${prisoner.lastName}",
      AppStatus.PENDING,
      staff.establishmentId
    )
  }

  private fun convertAppToAppResponseDto(
    app: App,
    prisoner: Any,
    assignedGroup: Any,
  ): AppResponseDto {
    return AppResponseDto(
      app.id,
      app.reference,
      assignedGroup,
      app.appType,
      app.requestedDate,
      app.createdDate,
      app.createdBy,
      app.lastModifiedDate,
      app.lastModifiedBy,
      app.comments,
      app.requests,
      prisoner,
      app.requestedByFullName,
      app.status,
    )
  }

  private fun convertAppToAppListDto(apps: List<App>): List<AppListViewDto> {
    val list = ArrayList<AppListViewDto>()
    apps.forEach { app ->
      val group = groupsService.getGroupById(app.assignedGroup)
      val groupAppListviewDto = GroupAppListViewDto(group.id, group.name)
      val appResponseDto = AppListViewDto(
        app.id,
        app.establishmentId,
        app.status.toString(),
        app.appType.toString(),
        app.requestedBy,
        app.requestedDate,
        groupAppListviewDto,
      )
      list.add(appResponseDto)
    }
    return list
  }

  private fun convertAppTypeCountsToMap(appByAppTypeCounts: List<AppByAppTypeCounts>): Map<AppType, Int> {
    val map = TreeMap<AppType, Int>()
    var appTypes = AppType.entries.toSet()
    appByAppTypeCounts.forEach { appTypeCount ->
      map[appTypeCount.getAppType()] = appTypeCount.getCount()
      appTypes = appTypes.minus(appTypeCount.getAppType())
    }
    appTypes.forEach { appType ->
      map[appType] = 0
    }
    return map
  }
}
