package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AssignedGroupDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import java.time.LocalDateTime
import java.util.*

@Service
class AppServiceImpl(
  var appRepository: AppRepository,
  var prisonerService: PrisonerService,
  var staffService: StaffService,
) : AppService {

  fun saveApp(app: App): App {
    return appRepository.save(app)
  }

  fun updateApp(app: App): App {
    return appRepository.save(app)
  }

  fun getAppByID(id: UUID): Optional<App> {
    return appRepository.findById(id)
  }

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
    val assignedGroup = AssignedGroupDto(
      null,
      EstablishmentDto("k", "kk"),
      null,
      null,
      null,
    )
    return convertAppToAppResponseDto(prisonerId, app, prisonerId, assignedGroup)
  }

  override fun getAppsById(prisonerId: String, id: UUID, requestedBy: Boolean, assignedGroup: Boolean): AppResponseDto {
    val app = appRepository.findById(id)
      .orElseThrow<ApiException> { throw ApiException("No app exist with id $id", HttpStatus.NOT_FOUND) }
    val assignedGroup = AssignedGroupDto(
      null,
      EstablishmentDto("k", "kk"),
      null,
      null,
      null,
    )
    val prisoner: Any
    if (requestedBy) {
      prisoner = prisonerService.getPrisonerById(prisonerId)
        .orElseThrow { throw ApiException("No prisoner exist with id $prisonerId", HttpStatus.NOT_FOUND) }
    } else {
      prisoner = prisonerId
    }

    return convertAppToAppResponseDto(prisonerId, app, prisoner, assignedGroup)
  }

  override fun getAppsByEstablishment(name: String): AppResponseDto {
    TODO("Not yet implemented")
  }

  private fun convertAppRequestToAppEntity(prisoner: Prisoner, staff: Staff, appRequest: AppRequestDto): App {
    // TODO("Not yet implemented")
    val localDateTime = LocalDateTime.now()
    return App(
      UUID.randomUUID(),
      appRequest.reference,
      UUID.randomUUID(),
      AppType.getAppType(appRequest.type),
      localDateTime,
      localDateTime,
      staff.id,
      arrayListOf(),
      appRequest.requests,
      localDateTime,
      staff.id,
      UUID.randomUUID(),
    )
  }

  private fun convertAppToAppResponseDto(
    prisonerId: String,
    app: App,
    prisoner: Any,
    assignedGroup: AssignedGroupDto,
  ): AppResponseDto {
    // TODO("Not yet implemented")

    return AppResponseDto(
      app.id,
      app.reference,
      assignedGroup,
      app.appType,
      app.createdDate,
      app.lastModifiedDateTime,
      app.lastModifiedBy,
      app.comments,
      app.requests,
      app.requestedDateTime,
      prisoner,
    )
  }
}
