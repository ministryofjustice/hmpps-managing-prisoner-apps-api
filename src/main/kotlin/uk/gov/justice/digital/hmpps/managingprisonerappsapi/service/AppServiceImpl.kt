package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import com.fasterxml.uuid.Generators
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppStatusUpdateDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppUpdateDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.CommentRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.FileRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppListViewDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppResponseListDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.ApplicationGroupResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.ApplicationTypeResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AssignedGroupDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.FileResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.GroupAppListViewDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.HistoryResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppByAppTypeCounts
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppByAssignedGroupCounts
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppFile
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationGroup
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.CommentVisibility
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Department
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.RequestedByNameSearchResult
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationGroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationTypeRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.stats.AppStatsContext
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.stats.MessageType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.stats.StatsTelemetryService
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class AppServiceImpl(
  private val appRepository: AppRepository,
  private val prisonerService: PrisonerService,
  private val staffService: StaffService,
  private val commentRepository: CommentRepository,
  private val activityService: ActivityService,
  private val historyService: HistoryService,
  private val establishmentService: EstablishmentService,
  private val applicationGroupRepository: ApplicationGroupRepository,
  private val applicationTypeRepository: ApplicationTypeRepository,
  private val statsTelemetryService: StatsTelemetryService,
  private val establishmentDepartmentService: EstablishmentDepartmentService,
  private val establishmentApplicationTypeService: EstablishmentApplicationTypeService,
) : AppService {

  companion object {
    private val logger = LoggerFactory.getLogger(AppServiceImpl::class.java)
  }

  override fun saveApp(app: App): App = appRepository.save(app)

  override fun updateAppFormData(
    prisonerId: String,
    staffId: String,
    appId: UUID,
    appUpdateDto: AppUpdateDto,
  ): AppResponseDto<Any, Any> {
    if (appUpdateDto.formData.size > 1 || appUpdateDto.formData.isEmpty()) {
      throw ApiException("Multiple or zero requests in app is not supported", HttpStatus.FORBIDDEN)
    }
    val staff = staffService.getStaffById(staffId).orElseThrow {
      ApiException("Staff with id $staffId not found", HttpStatus.FORBIDDEN)
    }
    var app = appRepository.findAppsByIdAndRequestedBy(appId, prisonerId)
      .orElseThrow<ApiException> { throw ApiException("No app exist with id $appId", HttpStatus.NOT_FOUND) }
    validateStaffPermission(staff, app)

    if (app.status != AppStatus.NEW && app.status != AppStatus.IN_PROGRESS) {
      throw ApiException("App is closed and cannot be updated", HttpStatus.FORBIDDEN)
    }
    appUpdateDto.formData.forEach { l ->
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
    app.lastModifiedDate = LocalDateTime.now(ZoneOffset.UTC)
    app.lastModifiedBy = staffId
    app.firstNightCenter = appUpdateDto.firstNightCenter
    app = appRepository.save(app)
    // val group = groupsService.getGroupById(app.assignedGroup, staff.establishmentId)
    val department = getDepartment(app)

    activityService.addActivity(
      app.id,
      EntityType.APP,
      app.id,
      Activity.APP_REQUEST_FORM_DATA_UPDATED,
      app.establishmentId,
      staffId,
      LocalDateTime.now(ZoneOffset.UTC),
      prisonerId,
      app.applicationType!!,
      app.applicationGroup!!,
      // group.name,
      department.name,
      "",
    )
    val applicationGroup = applicationGroupRepository.findById(app.applicationGroup!!).orElseThrow {
      throw ApiException("No applicationGroup found with id ${app.applicationGroup}", HttpStatus.NOT_FOUND)
    }
    val applicationType = applicationTypeRepository.findById(app.applicationType!!).orElseThrow {
      throw ApiException("No applicationType found with id ${app.applicationType}", HttpStatus.NOT_FOUND)
    }
    return convertAppToAppResponseDto(app, app.requestedBy, department, applicationGroup, applicationType)
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
    if (appRequestDto.department == null) {
      throw ApiException("Department is required for app submission", HttpStatus.BAD_REQUEST)
    }

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
    validateActiveApplicationGroupAndType(
      prisoner.establishmentId,
      appRequestDto.applicationGroup!!,
      appRequestDto.applicationType!!,
    )

    if (appRequestDto.requests.size != 1) {
      throw ApiException("Only one requests in app is supported", HttpStatus.BAD_REQUEST)
    }
    val establishment = establishmentService.getEstablishmentById(staff.establishmentId).orElseThrow {
      ApiException("The establishment: $staff.establishmentId is not enabled", HttpStatus.FORBIDDEN)
    }

/*    val establishmentId = if (establishment.defaultDepartments) "DEFAULT" else staff.establishmentId
    var groupId: UUID? = null
    if (appRequestDto.department != null) {
      groupsService.getGroupById(appRequestDto.department, staff.establishmentId)
      val groups = groupsService.getGroupByInitialAppType(establishmentId, appRequestDto.applicationType)
      if (groups.size <= 1) {
        groupId = appRequestDto.department
      } else {
        groups.forEach { d ->
          if (d.id == appRequestDto.department) {
            groupId = d.id
          }
        }
      }
      if (groupId == null) {
        throw ApiException("Department  with id ${appRequestDto.department} is not found", HttpStatus.FORBIDDEN)
      }
    } else {
      groupId = groupsService.getGroupByInitialAppType(establishmentId, appRequestDto.applicationType!!).first().id
    }*/

    val department = establishmentDepartmentService.findDepartmentByDepartmentIdAndEstablishmentId(appRequestDto.department!!, staff.establishmentId)
    var app = convertAppRequestToAppEntity(prisoner, staff, department, appRequestDto)
    // val assignedGroup = groupsService.getGroupById(groupId!!, staff.establishmentId)

    logger.info("Saving app request in db")
    app.appFiles.forEach { file -> file.app = app }
    app = appRepository.save(app)
    logger.info("App created for $prisonerId for app type ${app.appType}")
    val createdDate = LocalDateTime.now(ZoneOffset.UTC)
    activityService.addActivity(
      app.id,
      EntityType.ASSIGNED_GROUP,
      app.id,
      Activity.APP_SUBMITTED,
      app.establishmentId,
      staffId,
      createdDate,
      prisonerId,
      app.applicationType!!,
      app.applicationGroup!!,
      department.name,
      "",
    )
    app.appFiles.forEach { f ->
      activityService.addActivity(
        f.id,
        EntityType.FILE,
        app.id,
        Activity.FILE_ADDED,
        app.establishmentId,
        staffId,
        createdDate,
        prisonerId,
        app.applicationType!!,
        app.applicationGroup!!,
        department.name,
        "",
      )
    }
    val applicationGroup = applicationGroupRepository.findById(app.applicationGroup!!).orElseThrow {
      throw ApiException("No applicationGroup found with id ${app.applicationGroup}", HttpStatus.NOT_FOUND)
    }
    val applicationType = applicationTypeRepository.findById(app.applicationType!!).orElseThrow {
      throw ApiException("No applicationType found with id ${app.applicationType}", HttpStatus.NOT_FOUND)
    }
    return convertAppToAppResponseDto(app, prisonerId, department, applicationGroup, applicationType)
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
    val staff = staffService.getStaffById(staffId).orElseThrow {
      ApiException("Staff with id $staffId not found", HttpStatus.FORBIDDEN)
    }
    validateStaffPermission(staff, app)
    val department = getDepartment(app)
/*    //val groups = groupsService.getGroupById(app.assignedGroup, staff.establishmentId)
    val groupsDto: Any
    if (assignedGroup) {
      groupsDto = groups
    } else {
      groupsDto = groups.id
    }*/
    val prisoner: Any
    if (requestedBy) {
      prisoner = prisonerService.getPrisonerById(prisonerId)
        .orElseThrow { throw ApiException("No prisoner exist with id $prisonerId", HttpStatus.NOT_FOUND) }
    } else {
      prisoner = prisonerId
    }
    val applicationGroup = applicationGroupRepository.findById(app.applicationGroup!!).orElseThrow {
      throw ApiException("No applicationGroup found with id ${app.applicationGroup}", HttpStatus.NOT_FOUND)
    }
    val applicationType = applicationTypeRepository.findById(app.applicationType!!).orElseThrow {
      throw ApiException("No applicationType found with id ${app.applicationType}", HttpStatus.NOT_FOUND)
    }
    return convertAppToAppResponseDto(app, prisoner, department, applicationGroup, applicationType)
  }

  override fun getHistoryAppsId(prisonerId: String, appId: UUID, staffId: String): List<HistoryResponse> {
    val staff = staffService.getStaffById(staffId).orElseThrow {
      ApiException("Staff with id $staffId not found", HttpStatus.FORBIDDEN)
    }
    return historyService.getHistoryByAppId(appId, staff.establishmentId)
  }

  override fun forwardAppToGroup(
    staffId: String,
    departmentSlug: String,
    appId: UUID,
    commentRequestDto: CommentRequestDto?,
  ): AppResponseDto<Any, Any> {
    val staff = staffService.getStaffById(staffId).orElseThrow {
      ApiException("Staff with id $staffId not found", HttpStatus.FORBIDDEN)
    }
    val app = appRepository.findById(appId)
      .orElseThrow { throw ApiException("No app found with id $appId", HttpStatus.FORBIDDEN) }
    validateStaffPermission(staff, app)
    val establishment = establishmentService.getEstablishmentById(staff.establishmentId).orElseThrow {
      ApiException("The establishment: $staff.establishmentId is not enabled", HttpStatus.FORBIDDEN)
    }
    if (departmentSlug == app.departmentSlug) {
      throw ApiException("App already assigned to group $departmentSlug", HttpStatus.BAD_REQUEST)
    }
    var comment: Comment? = null
    if (commentRequestDto != null && commentRequestDto.message.isNotEmpty()) {
      comment = commentRepository.save(
        Comment(
          Generators.timeBasedEpochGenerator().generate(),
          commentRequestDto.message,
          LocalDateTime.now(ZoneOffset.UTC),
          staffId,
          appId,
          CommentVisibility.STAFF_ONLY,
          UserCategory.STAFF,
        ),
      )
    }
    val originalDepartment = getDepartment(app)
    val newDepartment = establishmentDepartmentService.findBySlugIgnoreCase(departmentSlug)

    app.departmentSlug = newDepartment.slug
    app.lastModifiedDate = LocalDateTime.now(ZoneOffset.UTC)
    app.lastModifiedBy = staffId
    val createdDate = LocalDateTime.now(ZoneOffset.UTC)

    appRepository.save(app)
    activityService.addActivity(
      newDepartment.id,
      EntityType.ASSIGNED_GROUP,
      app.id,
      Activity.APP_FORWARDED_TO_A_GROUP,
      app.establishmentId,
      staffId,
      createdDate,
      app.requestedBy,
      app.applicationType!!,
      app.applicationGroup!!,
      newDepartment.name,
      "",
    )
    if (comment != null) {
      activityService.addActivity(
        comment.id,
        EntityType.COMMENT,
        app.id,
        Activity.FORWARDING_COMMENT_ADDED,
        app.establishmentId,
        staffId,
        createdDate,
        app.requestedBy,
        app.applicationType!!,
        app.applicationGroup!!,
        originalDepartment.name,
        "",
      )
    }

    var establishmentName = establishment.name
    val appTypeName = applicationTypeRepository.findById(app.applicationType!!).map { it.name }.orElse(null)
    val appGroupName = applicationGroupRepository.findById(app.applicationGroup!!).map { it.name }.orElse(null)

    val appStatsContext = AppStatsContext(
      app.id,
      establishmentName,
      app.applicationType!!,
      appTypeName,
      app.applicationGroup!!,
      appGroupName,
      newDepartment.name,
    )
    statsTelemetryService.logMessageAdded(
      appStatsContext,
      UserCategory.STAFF,
      CommentVisibility.STAFF_ONLY,
      MessageType.FORWARDING_COMMENT,
      createdDate,
    )

    val applicationGroup = applicationGroupRepository.findById(app.applicationGroup!!).orElseThrow {
      throw ApiException("No applicationGroup found with id ${app.applicationGroup}", HttpStatus.NOT_FOUND)
    }
    val applicationType = applicationTypeRepository.findById(app.applicationType!!).orElseThrow {
      throw ApiException("No applicationType found with id ${app.applicationType}", HttpStatus.NOT_FOUND)
    }
    return convertAppToAppResponseDto(app, app.requestedBy, newDepartment, applicationGroup, applicationType)
  }

  override fun searchAppsByColumnsFilter(
    staffId: String,
    status: Set<AppStatus>,
    appTypes: Set<Long>?,
    requestedBy: String?,
    assignedGroups: Set<String>?,
    firstNightCenter: Boolean?,
    pageNumber: Long,
    pageSize: Long,
    oldestFirst: Boolean,
  ): AppResponseListDto {
    val staff = staffService.getStaffById(staffId).orElseThrow {
      throw ApiException("No staff with id $staffId", HttpStatus.BAD_REQUEST)
    }
    var establishment = establishmentService.getEstablishmentById(staff.establishmentId).orElseThrow {
      ApiException("The establishment: $staff.establishmentId is not enabled", HttpStatus.FORBIDDEN)
    }
    val establishmentId = if (establishment.defaultDepartments) "DEFAULT" else staff.establishmentId
    var appTypeDto: List<AppByAppTypeCounts> = listOf()
    var assignedGroupTypesCounts: List<AppByAssignedGroupCounts> = listOf()
    var appByFirstNightCount: Int = 0
    var pageResult: Page<App> = Page.empty()
    runBlocking {
      launch {
        establishment = establishmentService.getEstablishmentById(staff.establishmentId).orElseThrow {
          ApiException("Establishment with id: ${staff.establishmentId} is not enabled.", HttpStatus.FORBIDDEN)
        }
      }
      launch {
        appTypeDto = appRepository.countBySearchFilterGroupByAppType(
          staff.establishmentId,
          status,
          appTypes,
          requestedBy,
          assignedGroups,
          firstNightCenter,
        )
      }
      launch {
        assignedGroupTypesCounts = appRepository.countBySearchFilterGroupByAssignedGroup(
          staff.establishmentId,
          status,
          appTypes,
          requestedBy,
          assignedGroups,
          firstNightCenter,
        )
      }
      launch {
        appByFirstNightCount = appRepository.countBySearchFilterGroupByFirstNightCenter(
          staff.establishmentId,
          status,
          appTypes,
          requestedBy,
          assignedGroups,
          true,
        ).getCount()
      }
      launch {
        var sortDirection: Sort.Direction? = null
        if (oldestFirst) {
          sortDirection = Sort.Direction.ASC
        } else {
          sortDirection = Sort.Direction.DESC
        }
        val pageRequest =
          PageRequest.of((pageNumber - 1).toInt(), pageSize.toInt()).withSort(sortDirection, "requestedDate")
        pageResult = appRepository.appsBySearchFilter(
          staff.establishmentId,
          status,
          appTypes,
          requestedBy,
          assignedGroups,
          firstNightCenter,
          pageRequest,
        )
      }
    }
    val appsList = convertAppToAppListDto(pageResult.content, staff.establishmentId)
    // val groups = groupsService.getGroupsByEstablishmentId(establishmentId, staff.establishmentId)
    val departments = establishmentDepartmentService.findDepartmentsForEstablishment(staff.establishmentId)
    val establishmentDto = establishmentService.getEstablishmentById(staff.establishmentId).orElseThrow {
      ApiException("The establishment: $staff.establishmentId is not enabled", HttpStatus.FORBIDDEN)
    }
    val assignedGroupDtoList = convertToAssignedGroupDtoList(departments, establishmentDto)

    return AppResponseListDto(
      pageResult.pageable.pageNumber + 1,
      pageResult.totalElements,
      pageResult.isLast,
      convertAppTypeCountsToMap(appTypeDto),
      convertAssignedGroupCountsToGroupAppListViewDto(
        assignedGroupDtoList,
        assignedGroupTypesCounts,
      ),
      appByFirstNightCount.toLong(),
      appsList,
    )
  }

  override fun updateAppStatusToInProgress(
    prisonerId: String,
    staffId: String,
    appId: UUID,
    appStatusUpdateDto: AppStatusUpdateDto,
  ): AppResponseDto<Any, Any> {
    if (appStatusUpdateDto.status != AppStatus.IN_PROGRESS) {
      throw ApiException("App Status can only be updated to IN_PROGRESS", HttpStatus.FORBIDDEN)
    }
    var app = appRepository.findAppsByIdAndRequestedBy(appId, prisonerId)
      .orElseThrow<ApiException> { throw ApiException("No app exist with id $appId", HttpStatus.NOT_FOUND) }

    if (app.status != AppStatus.NEW) {
      throw ApiException("App Status cannot be updated", HttpStatus.FORBIDDEN)
    }
    val staff = staffService.getStaffById(staffId).orElseThrow {
      ApiException("Staff with id $staffId not found", HttpStatus.FORBIDDEN)
    }

    validateStaffPermission(staff, app)

    val applicationGroup = applicationGroupRepository.findById(app.applicationGroup!!).orElseThrow {
      throw ApiException("No applicationGroup found with id ${app.applicationGroup}", HttpStatus.NOT_FOUND)
    }
    val applicationType = applicationTypeRepository.findById(app.applicationType!!).orElseThrow {
      throw ApiException("No applicationType found with id ${app.applicationType}", HttpStatus.NOT_FOUND)
    }

    val createdDate = LocalDateTime.now(ZoneOffset.UTC)
    val fromStatus = app.status
    app.lastModifiedDate = createdDate
    app.lastModifiedBy = staffId
    app.status = appStatusUpdateDto.status
    app = appRepository.save(app)

    // val group = groupsService.getGroupById(app.assignedGroup, staff.establishmentId)
    val department = getDepartment(app)
    val establishment = establishmentApplicationTypeService.findByDepartmentSlugAndEstablishmentId(app.departmentSlug, app.establishmentId).establishment

    activityService.addActivity(
      app.id,
      EntityType.ASSIGNED_GROUP,
      app.id,
      Activity.APP_IN_PROGRESS,
      app.establishmentId,
      staffId,
      createdDate,
      prisonerId,
      app.applicationType!!,
      app.applicationGroup!!,
      department.name,
      "",
    )

    val toStatus = app.status

    val appTypeName = applicationTypeRepository.findById(app.applicationType!!).map { it.name }.orElse(null)
    val appGroupName = applicationGroupRepository.findById(app.applicationGroup!!).map { it.name }.orElse(null)

    val appStatsContext = AppStatsContext(
      app.id,
      establishment.name,
      app.applicationType!!,
      appTypeName,
      app.applicationGroup!!,
      appGroupName,
      department.name,
    )
    statsTelemetryService.logStatusChanged(
      appStatsContext,
      fromStatus,
      toStatus,
      createdDate,
    )
    return convertAppToAppResponseDto(app, app.requestedBy, department, applicationGroup, applicationType)
  }

  override fun searchRequestedByTextSearch(staffId: String, text: String): List<RequestedByNameSearchResult> {
    if (text.isBlank() || text.length < 3) {
      throw ApiException("Text search cannot be empty or just whitespaces or less than 3 chars", HttpStatus.BAD_REQUEST)
    }
    val staff = staffService.getStaffById(staffId)
      .orElseThrow { throw ApiException("No staff with id $staffId found", HttpStatus.NOT_FOUND) }
    val result = appRepository.searchRequestedByFirstOrLastName(staff.establishmentId, text)
    if (result.size <= 20) {
      return result
    } else {
      return result.subList(0, 19)
    }
  }

  private fun convertAppRequestToAppEntity(
    prisoner: Prisoner,
    staff: Staff,
    department: Department,
    appRequest: AppRequestDto,
  ): App {
    val localDateTime = LocalDateTime.now(ZoneOffset.UTC)
    var firstNightCenter = false
    if (appRequest.applicationType == 3L && appRequest.firstNightCenter != null) {
      firstNightCenter = appRequest.firstNightCenter
    }
    return App(
      Generators.timeBasedEpochGenerator().generate(), // id
      appRequest.reference, // reference
      null,
      department.id, // department id
      department.slug, // department slug
      null,
      appRequest.applicationGroup,
      appRequest.applicationType, // created date
      appRequest.genericForm,
      appRequest.requestedDate ?: localDateTime, // last modified date
      localDateTime, // created by
      staff.username,
      UserCategory.STAFF,
      localDateTime,
      staff.username,
      convertRequestsToAppRequests(appRequest.requests),
      prisoner.username,
      prisoner.firstName,
      prisoner.lastName,
      AppStatus.NEW,
      prisoner.establishmentId!!,
      firstNightCenter,
      convertAppFilestoAppFileEnityList(appRequest.fileRequestDtos, staff.username) as MutableList<AppFile>,
    )
  }

  private fun convertToAssignedGroupDtoList(departments: List<Department>, establishmentDto: EstablishmentDto): List<AssignedGroupDto> {
    val list = ArrayList<AssignedGroupDto>()
    departments.forEach { department ->
      list.add(AssignedGroupDto(department.slug, department.name, establishmentDto, null, null))
    }
    return list
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
    applicationGroup: ApplicationGroup,
    applicationType: ApplicationType,
  ): AppResponseDto<Any, Any> = AppResponseDto(
    app.id,
    app.reference,
    assignedGroup,
    null,
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
    app.firstNightCenter,
    convertEntityFilesToFiles(app.appFiles),
  )

  /**
   * Convert a list of App entities from app search.
   */
  private fun convertAppToAppListDto(apps: List<App>, establishmentId: String): List<AppListViewDto> {
    val list = ArrayList<AppListViewDto>()
    apps.forEach { app ->
      var groupAppListviewDto: GroupAppListViewDto? = null
      var applicationType: ApplicationType? = null
      var commentNum: Long = 0
      runBlocking {
        launch {
          // val group = groupsService.getGroupById(app.assignedGroup, establishmentId)
          val department = getDepartment(app)
          groupAppListviewDto = GroupAppListViewDto(department.slug, department.name, null)
        }
        launch {
          applicationType = applicationTypeRepository.findById(app.applicationType!!).orElseThrow {
            ApiException("No app type found for ${app.applicationType}", HttpStatus.NOT_FOUND)
          }
        }
        launch {
          commentNum = commentRepository.countCommentsByAppId(app.id)
        }
      }

      val appResponseDto = AppListViewDto(
        app.id,
        app.establishmentId,
        app.status.toString(),
        ApplicationTypeResponse(applicationType!!.id, applicationType.name, null, null, null, null),
        app.genericForm,
        app.requestedBy,
        app.requestedByFirstName,
        app.requestedByLastName,
        app.createdDate,
        groupAppListviewDto!!,
        commentNum,
      )
      list.add(appResponseDto)
    }
    return list
  }

  private fun convertAppTypeCountsToMap(appByAppTypeCounts: List<AppByAppTypeCounts>): Map<Long, ApplicationTypeResponse> {
    val map = TreeMap<Long, ApplicationTypeResponse>()
    appByAppTypeCounts.forEach { appTypeCount ->
      val applicationType = applicationTypeRepository.findById(appTypeCount.getApplicationType()).orElseThrow {
        ApiException("No app type with id $appTypeCount", HttpStatus.NOT_FOUND)
      }
      map[applicationType.id] = ApplicationTypeResponse(
        applicationType.id,
        applicationType.name,
        null,
        null,
        null,
        appTypeCount.getCount().toLong(),
      )
    }
    return map
  }

  private fun convertAppFilestoAppFileEnityList(
    appFileRequestDtos: List<FileRequestDto>,
    createdBy: String,
  ): List<AppFile> {
    val files = ArrayList<AppFile>()
    appFileRequestDtos.forEach { file ->
      files.add(
        AppFile(
          Generators.timeBasedEpochGenerator().generate(),
          file.documentId.toString(),
          file.fileName,
          LocalDateTime.now(ZoneOffset.UTC),
          createdBy,
          file.fileType,
        ),
      )
    }
    return files
  }

  private fun convertEntityFilesToFiles(appFiles: List<AppFile>): List<FileResponseDto> {
    val files = ArrayList<FileResponseDto>()
    appFiles.forEach { file ->
      files.add(
        FileResponseDto(
          file.id,
          UUID.fromString(file.documentId),
          file.fileName,
          LocalDateTime.now(ZoneOffset.UTC),
          file.createdBy,
          file.fileType,
        ),
      )
    }
    return files
  }

  private fun convertAssignedGroupCountsToGroupAppListViewDto(
    groups: List<AssignedGroupDto>,
    countByGroups: List<AppByAssignedGroupCounts>,
  ): List<GroupAppListViewDto> {
    val list = ArrayList<GroupAppListViewDto>()
    val map = HashMap<String, Long>()
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
      throw ApiException(
        "Staff with id ${staff.username} do not have permission to view other establishment App",
        HttpStatus.FORBIDDEN,
      )
    }
  }

  private fun validateActiveApplicationGroupAndType(establishmentId: String, appGroupId: Long, appTypeId: Long) {
    val appTypeView = establishmentApplicationTypeService.findApplicationTypeForEstablishment(establishmentId, appTypeId)
    if (!appTypeView.active) {
      throw ApiException(
        "Application type ${appTypeView?.applicationTypeId} is not enabled for establishment}",
        HttpStatus.FORBIDDEN,
      )
    } else if (appTypeView.applicationGroupId != appGroupId) {
      throw ApiException(
        "Application group ${appTypeView?.applicationGroupId} does not belong to group $appGroupId",
        HttpStatus.FORBIDDEN,
      )
    }
  }

  private fun getDepartment(app: App): Department = establishmentDepartmentService.findBySlugIgnoreCase(app.departmentSlug)
}
