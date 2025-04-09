package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppDecisionRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppDecisionResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.StaffDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
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
) : ResponseService {

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
    val reqs = ArrayList<Map<String, Any>>()
    var responseEntity: Response? = null
    app.requests.forEach { request ->
      val req = request.toMutableMap()
      if (response.appliesTo.contains(UUID.fromString(request.get("id") as String))) {
        responseEntity = responseRepository.save(
          Response(
            UUID.randomUUID(),
            response.reason,
            response.decision,
            LocalDateTime.now(ZoneOffset.UTC),
            staffId,
          ),
        )
        req["responseId"] = responseEntity!!.id.toString()
      }
      reqs.add(req)
    }
    app.requests = reqs
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
    if (createdBy) {
      val establishment = establishmentService.getEstablishmentById(staff.establishmentId).orElseThrow {
        ApiException(
          "Staff establishment ${staff.establishmentId} not added in establishment record",
          HttpStatus.BAD_REQUEST,
        )
      }
      staffDto = StaffDto(
        staff.username,
        staff.userId,
        "${staff.fullName}",
        UserCategory.STAFF,
        establishment,
      )
    } else {
      staffDto = staff.username
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
