package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppDecisionRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppDecisionResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.StaffDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Decision
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Response
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ResponseRepository
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class ResponseServiceImpl(
  private val appService: AppService,
  private val prisonerService: PrisonerService,
  private val staffService: StaffService,
  private val responseRepository: ResponseRepository,
  private val establishmentService: EstablishmentService,
  private val activityService: ActivityService,
) : ResponseService {

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun addResponse(
    prisonerId: String,
    appId: UUID,
    staffId: String,
    response: AppDecisionRequestDto,
  ): AppDecisionResponseDto<Any> {
    val staff = getStaff(staffId)
    val app = appService.getAppById(appId).orElseThrow {
      ApiException("No app found for id $appId", HttpStatus.NOT_FOUND)
    }
    validateStaffPermission(staff, app)
    validatePrisonerByRequestedBy(prisonerId, app)
    if (response.appliesTo.size > 1) {
      throw ApiException("Multiple responses is not supported as multiple request is not supported", HttpStatus.FORBIDDEN)
    }
    val reqs = ArrayList<MutableMap<String, Any>>()
    var responseEntity: Response? = null
    app.requests.forEach { request ->
      val req = request.toMutableMap()
      if (response.appliesTo.contains(UUID.fromString(request.get("id") as String))) {
        if (request.get("responseId") != null) {
          throw ApiException("Response already added for request id: ${request.get("id")}", HttpStatus.BAD_REQUEST)
        }
        responseEntity = responseRepository.save(
          Response(
            UUID.randomUUID(),
            response.reason,
            response.decision,
            LocalDateTime.now(ZoneOffset.UTC),
            staffId,
          ),
        )
        val activity = if (responseEntity!!.decision == Decision.APPROVED) Activity.APP_APPROVED else Activity.APP_DECLINED
        activityService.addActivity(
          responseEntity!!.id,
          EntityType.RESPONSE,
          app.id,
          activity,
          app.establishmentId,
          staffId,
          LocalDateTime.now(ZoneOffset.UTC),
          prisonerId,
          app.appType,
        )
        req["responseId"] = responseEntity!!.id.toString()
        app.responses.add(responseEntity!!.id)
      }
      reqs.add(req)
    }
    app.requests = reqs
    if (app.requests.size == app.responses.size) {
      if (response.decision == Decision.APPROVED) {
        app.status = AppStatus.APPROVED
      }
      if (response.decision == Decision.DECLINED) {
        app.status = AppStatus.DECLINED
      }
    }
    appService.saveApp(app)

    return convertResponseToAppDecisionResponse(prisonerId, staff.username, response.appliesTo, app.id, responseEntity!!)
  }

  override fun getResponseById(
    prisonerId: String,
    appId: UUID,
    staffId: String,
    createdBy: Boolean,
    responseId: UUID,
  ): AppDecisionResponseDto<Any> {
    val staff = getStaff(staffId)
    val staffDto: Any
    val app = appService.getAppById(appId).orElseThrow {
      ApiException("No app found for id $appId", HttpStatus.NOT_FOUND)
    }
    validateStaffPermission(staff, app)
    validatePrisonerByRequestedBy(prisonerId, app)
    if (app.requestedBy != prisonerId) {
      throw ApiException("Not applicable for id $prisonerId", HttpStatus.FORBIDDEN)
    }
    val appliesTo = ArrayList<UUID>()
    app.requests.forEach { request ->
      if (UUID.fromString(request["responseId"]!! as String).equals(responseId)) {
        appliesTo.add(UUID.fromString(request["id"] as String))
      }
    }
    val response = responseRepository.findById(responseId).orElseThrow {
      ApiException("No response found for id $responseId", HttpStatus.NOT_FOUND)
    }
    val recordCreatedBy = staffService.getStaffById(response.createdBy).orElseThrow {
      ApiException("Record of by satff: ${response.createdBy} not found", HttpStatus.BAD_REQUEST)
    }
    if (createdBy) {
      val establishment = establishmentService.getEstablishmentById(recordCreatedBy.establishmentId).orElseThrow {
        ApiException(
          "Staff establishment ${staff.establishmentId} not added in establishment record",
          HttpStatus.BAD_REQUEST,
        )
      }
      staffDto = StaffDto(
        recordCreatedBy.username,
        recordCreatedBy.userId,
        "${recordCreatedBy.fullName}",
        UserCategory.STAFF,
        establishment,
      )
    } else {
      staffDto = recordCreatedBy.username
    }
    return convertResponseToAppDecisionResponse(prisonerId, staffDto, appliesTo, appId, response)
  }

  private fun convertResponseToAppDecisionResponse(
    prisonerId: String,
    staff: Any,
    requestIds: List<UUID>,
    appId: UUID,
    response: Response,
  ): AppDecisionResponseDto<Any> = AppDecisionResponseDto(
    response.id,
    prisonerId,
    appId,
    response.reason,
    response.decision,
    response.createdDate,
    staff,
    requestIds,
  )

  private fun getStaff(staffId: String): Staff = staffService.getStaffById(staffId).orElseThrow {
    ApiException("Staff with id $staffId does not exist", HttpStatus.NOT_FOUND)
  }

  private fun validateStaffPermission(staff: Staff, app: App) {
    if (staff.establishmentId != app.establishmentId) {
      throw ApiException("Staff with id ${staff.username}do not have permission to view other establishment App", HttpStatus.FORBIDDEN)
    }
  }

  private fun validatePrisonerByRequestedBy(prisonerId: String, app: App) {
    if (prisonerId != app.requestedBy) {
      throw ApiException("App with id ${app.id} is not requested by $prisonerId", HttpStatus.FORBIDDEN)
    }
  }
}
