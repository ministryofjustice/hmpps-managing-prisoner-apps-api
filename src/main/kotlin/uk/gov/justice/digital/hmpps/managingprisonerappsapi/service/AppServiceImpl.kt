package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppListViewDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppResponseListDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AssignedGroupDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.GroupAppListViewDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppByAppTypeCounts
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppByAssignedGroupCounts
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
) : AppService {

  companion object {
    private val logger = LoggerFactory.getLogger(AppResource::class.java)
  }

  override fun saveApp(app: App): App = appRepository.save(app)

  override fun updateAppFormData(
    prisonerId: String,
    staffId: String,
    appId: UUID,
    requestFormData: List<Map<String, Any>>,
  ): AppResponseDto<Any, Any> {
    if (requestFormData.size > 1 || requestFormData.isEmpty()) {
      throw ApiException("Multiple or zero requests in app is not supported", HttpStatus.FORBIDDEN)
    }
    val staff = staffService.getStaffById(staffId).orElseThrow {
      ApiException("Staff with id $staffId not found", HttpStatus.NOT_FOUND)
    }
    var app = appRepository.findAppsByIdAndRequestedBy(appId, prisonerId)
      .orElseThrow<ApiException> { throw ApiException("No app exist with id $appId", HttpStatus.NOT_FOUND) }
    validateStaffPermission(staff, app)

    if (app.status != AppStatus.PENDING) {
      throw ApiException("App is closed and cannot be updated", HttpStatus.FORBIDDEN)
    }
    requestFormData.forEach { l ->
      app.requests.forEach { req ->
        if (req["id"] == l["id"] && req["responseId"] == null) {
          req.keys.forEach { key ->
            if (key != "id" && l[key] != null) {
              req[key] = l[key] as Any
            }
          }
        }
      }
    }
    app = appRepository.save(app)
    return convertAppToAppResponseDto(app, app.requestedBy, app.assignedGroup)
  }

  fun updateApp(app: App): App = appRepository.save(app)

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

  override fun getAppById(appId: UUID): Optional<App> = appRepository.findById(appId)

  override fun submitApp(prisonerId: String, staffId: String, appRequestDto: AppRequestDto): AppResponseDto<Any, Any> {
    // validate prisoner
    val prisoner = prisonerService.getPrisonerById(prisonerId).orElseThrow {
      ApiException("Prison with id $prisonerId not found", HttpStatus.NOT_FOUND)
    }
    val staff = staffService.getStaffById(staffId).orElseThrow {
      ApiException("Staff with id $staffId not found", HttpStatus.NOT_FOUND)
    }
    if (prisoner.establishmentId != staff.establishmentId) {
      throw ApiException("Staff and prisoner is from two different establishment", HttpStatus.FORBIDDEN)
    }
    if (appRequestDto.requests.size > 1) {
      throw ApiException("Multiple requests in app is not supported", HttpStatus.FORBIDDEN)
    }
    val group =
      groupsService.getGroupByInitialAppType(staff.establishmentId, AppType.getAppType(appRequestDto.type))
    var app = convertAppRequestToAppEntity(prisoner, staff, group.id, appRequestDto)
    val assignedGroup = groupsService.getGroupById(group.id)
    app = appRepository.save(app)
    logger.info("App created for $prisonerId for app type ${app.appType}")
    return convertAppToAppResponseDto(app, prisonerId, assignedGroup)
  }

  override fun getAppsById(
    prisonerId: String,
    appId: UUID,
    staffId: String,
    requestedBy: Boolean,
    assignedGroup: Boolean,
  ): AppResponseDto<Any, Any> {
    val app = appRepository.findAppsByIdAndRequestedBy(appId, prisonerId)
      .orElseThrow<ApiException> { throw ApiException("No app exist with id $appId", HttpStatus.NOT_FOUND) }
    if (prisonerId != app.requestedBy) {
      throw ApiException("The app with id $appId is not requested by $prisonerId", HttpStatus.FORBIDDEN)
    }
    val staff = staffService.getStaffById(staffId).orElseThrow {
      ApiException("Staff with id $staffId not found", HttpStatus.FORBIDDEN)
    }
    validateStaffPermission(staff, app)
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

  override fun forwardAppToGroup(staffId: String, groupId: UUID, appId: UUID): AppResponseDto<Any, Any> {
    val app = appRepository.findById(appId)
      .orElseThrow { throw ApiException("No app found with id $appId", HttpStatus.NOT_FOUND) }
    val staff = staffService.getStaffById(staffId).orElseThrow {
      ApiException("Staff with id $staffId not found", HttpStatus.FORBIDDEN)
    }
    validateStaffPermission(staff, app)
    val group = groupsService.getGroupById(groupId)
    app.assignedGroup = groupId
    appRepository.save(app)
    return convertAppToAppResponseDto(app, app.requestedBy, group)
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
    val appTypeDto = appRepository.countBySearchFilterGroupByAppType(
      staff.establishmentId,
      status,
      appTypes,
      requestedBy,
      assignedGroups,
    )
    val assignedGroupTypesCounts = appRepository.countBySearchFilterGroupByAssignedGroup(
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
      pageRequest,
    )
    val appsList = convertAppToAppListDto(pageResult.content)
    val groups = groupsService.getGroupsByEstablishmentId(staff.establishmentId)
    return AppResponseListDto(
      pageResult.pageable.pageNumber + 1,
      pageResult.totalElements,
      pageResult.isLast,
      convertAppTypeCountsToMap(appTypeDto),
      convertAssignedGroupCountsToGroupAppListViewDto(
        groups,
        assignedGroupTypesCounts,
      ),
      appsList,
    )
  }

  override fun searchRequestedByTextSearch(staffId: String, text: String): List<RequestedByNameSearchResult> {
    if (text.isBlank() || text.length < 3) {
      throw ApiException("Text search cannot be empty or just whitespaces or less than 3 chars", HttpStatus.BAD_REQUEST)
    }
    val staff = staffService.getStaffById(staffId)
      .orElseThrow { throw ApiException("No staff with id $staffId found", HttpStatus.NOT_FOUND) }
    return appRepository.searchRequestedByFirstOrLastName(staff.establishmentId, text)
  }

  private fun convertAppRequestToAppEntity(
    prisoner: Prisoner,
    staff: Staff,
    groupId: UUID,
    appRequest: AppRequestDto,
  ): App {
    val localDateTime = LocalDateTime.now(ZoneOffset.UTC)
    return App(
      UUID.randomUUID(), // id
      appRequest.reference, // reference
      groupId, // group id
      AppType.getAppType(appRequest.type),
      appRequest.requestedDate,
      localDateTime, // created date
      staff.username,
      localDateTime, // last modified date
      staff.username, // created by
      mutableListOf(),
      convertRequestsToAppRequests(appRequest.requests),
      prisoner.username,
      prisoner.firstName,
      prisoner.lastName,
      AppStatus.PENDING,
      staff.establishmentId,
      mutableListOf(),
    )
  }

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

  private fun convertAppToAppResponseDto(
    app: App,
    prisoner: Any,
    assignedGroup: Any,
  ): AppResponseDto<Any, Any> = AppResponseDto(
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
    app.requestedByFirstName,
    app.requestedByLastName,
    app.status,
    app.establishmentId,
    app.responses,
  )

  private fun convertAppToAppListDto(apps: List<App>): List<AppListViewDto> {
    val list = ArrayList<AppListViewDto>()
    apps.forEach { app ->
      val group = groupsService.getGroupById(app.assignedGroup)
      val groupAppListviewDto = GroupAppListViewDto(group.id, group.name, null)
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

  private fun convertAssignedGroupCountsToGroupAppListViewDto(
    groups: List<AssignedGroupDto>,
    countByGroups: List<AppByAssignedGroupCounts>,
  ): List<GroupAppListViewDto> {
    val list = ArrayList<GroupAppListViewDto>()
    val map = HashMap<UUID, Long>()
    countByGroups.forEach { groupCount ->
      map.put(groupCount.getAssignedGroup(), groupCount.getCount().toLong())
    }
    groups.forEach { group ->
      if (map.get(group.id) != null) {
        list.add(GroupAppListViewDto(group.id, group.name, map.get(group.id)))
      } else {
        list.add(GroupAppListViewDto(group.id, group.name, 0))
      }
    }
    return list
  }

  private fun validateStaffPermission(staff: Staff, app: App) {
    if (staff.establishmentId != app.establishmentId) {
      throw ApiException("Staff with id ${staff.username}do not have permission to view other establishment App", HttpStatus.FORBIDDEN)
    }
  }
}
