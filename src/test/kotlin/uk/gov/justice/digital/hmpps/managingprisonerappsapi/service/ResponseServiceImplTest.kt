package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import com.fasterxml.uuid.Generators
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppDecisionRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AssignedGroupDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationGroup
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Decision
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Response
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationGroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationTypeRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ResponseRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.stats.StatsTelemetryService
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class ResponseServiceImplTest {

  private val establishmentId = "TE1"
  private val requestedBy = "A12345"
  private val staffId = "STAFF1"
  private val assignedGroup = Generators.timeBasedEpochGenerator().generate()

  private lateinit var appService: AppService
  private lateinit var prisonerService: PrisonerService
  private lateinit var staffService: StaffService
  private lateinit var responseRepository: ResponseRepository
  private lateinit var applicationTypeRepository: ApplicationTypeRepository
  private lateinit var applicationGroupRepository: ApplicationGroupRepository
  private lateinit var establishmentService: EstablishmentService
  private lateinit var activityService: ActivityService
  private lateinit var groupService: GroupService
  private lateinit var statsTelemetryService: StatsTelemetryService

  private lateinit var responseServiceImpl: ResponseServiceImpl
  private lateinit var app: App

  @BeforeEach
  fun beforeEach() {
    appService = Mockito.mock(AppService::class.java)
    prisonerService = Mockito.mock(PrisonerService::class.java)
    staffService = Mockito.mock(StaffService::class.java)
    responseRepository = Mockito.mock(ResponseRepository::class.java)
    applicationTypeRepository = Mockito.mock(ApplicationTypeRepository::class.java)
    applicationGroupRepository = Mockito.mock(ApplicationGroupRepository::class.java)
    establishmentService = Mockito.mock(EstablishmentService::class.java)
    activityService = Mockito.mock(ActivityService::class.java)
    groupService = Mockito.mock(GroupService::class.java)
    statsTelemetryService = Mockito.mock(StatsTelemetryService::class.java)

    responseServiceImpl = ResponseServiceImpl(
      appService,
      prisonerService,
      staffService,
      responseRepository,
      applicationTypeRepository,
      applicationGroupRepository,
      establishmentService,
      activityService,
      groupService,
      statsTelemetryService,
    )

    app = DataGenerator.generateApp(
      establishmentId,
      null,
      1,
      1,
      requestedBy,
      LocalDateTime.now(ZoneOffset.UTC).minusDays(2),
      "Test",
      "Prisoner",
      AppStatus.NEW,
      assignedGroup,
      false,
    )
  }

  @Test
  fun `add response sets app last modified fields before saving`() {
    val originalLastModifiedDate = app.lastModifiedDate
    val requestId = UUID.randomUUID()
    app.requests = listOf(
      hashMapOf(
        "id" to requestId.toString(),
        "contact-number" to "1234567890",
      ),
    )

    val staff = Staff(
      staffId,
      UUID.randomUUID().toString(),
      "Prison Staff",
      UserCategory.STAFF,
      establishmentId,
      "Officer",
      UUID.randomUUID(),
    )
    val decisionRequest = AppDecisionRequestDto("All good, approved", Decision.APPROVED, listOf(requestId))
    val responseEntity = Response(
      UUID.randomUUID(),
      decisionRequest.reason,
      decisionRequest.decision,
      LocalDateTime.now(ZoneOffset.UTC),
      staffId,
      app.id,
    )

    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(appService.saveApp(any())).thenAnswer { it.arguments[0] as App }
    Mockito.`when`(responseRepository.save(any())).thenReturn(responseEntity)
    Mockito.`when`(applicationTypeRepository.findById(1L)).thenReturn(Optional.of(ApplicationType(1, "Type", false, false, false)))
    Mockito.`when`(applicationGroupRepository.findById(1L)).thenReturn(Optional.of(ApplicationGroup(1, "Group", emptyList())))
    Mockito.`when`(groupService.getGroupById(assignedGroup, establishmentId)).thenReturn(
      AssignedGroupDto(
        assignedGroup,
        "Business Hub",
        EstablishmentDto(establishmentId, "Test Establishment", false, emptySet(), emptySet()),
        1,
        GroupType.WING,
      ),
    )

    responseServiceImpl.addResponse(requestedBy, app.id, staffId, decisionRequest)

    Mockito.verify(appService).saveApp(app)
    assertEquals(staffId, app.lastModifiedBy)
    assertTrue(app.lastModifiedDate.isAfter(originalLastModifiedDate))
  }

  @Test
  fun `add rejected response sets app last modified fields before saving`() {
    val originalLastModifiedDate = app.lastModifiedDate
    val requestId = UUID.randomUUID()
    app.requests = listOf(
      hashMapOf(
        "id" to requestId.toString(),
        "contact-number" to "1234567890",
      ),
    )

    val staff = Staff(
      staffId,
      UUID.randomUUID().toString(),
      "Prison Staff",
      UserCategory.STAFF,
      establishmentId,
      "Officer",
      UUID.randomUUID(),
    )
    val decisionRequest = AppDecisionRequestDto("Missing details", Decision.REJECTED, listOf(requestId))
    val responseEntity = Response(
      UUID.randomUUID(),
      decisionRequest.reason,
      decisionRequest.decision,
      LocalDateTime.now(ZoneOffset.UTC),
      staffId,
      app.id,
    )

    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(appService.saveApp(any())).thenAnswer { it.arguments[0] as App }
    Mockito.`when`(responseRepository.save(any())).thenReturn(responseEntity)
    Mockito.`when`(applicationTypeRepository.findById(1L)).thenReturn(Optional.of(ApplicationType(1, "Type", false, false, false)))
    Mockito.`when`(applicationGroupRepository.findById(1L)).thenReturn(Optional.of(ApplicationGroup(1, "Group", emptyList())))
    Mockito.`when`(groupService.getGroupById(assignedGroup, establishmentId)).thenReturn(
      AssignedGroupDto(
        assignedGroup,
        "Business Hub",
        EstablishmentDto(establishmentId, "Test Establishment", false, emptySet(), emptySet()),
        1,
        GroupType.WING,
      ),
    )

    responseServiceImpl.addResponse(requestedBy, app.id, staffId, decisionRequest)

    Mockito.verify(appService).saveApp(app)
    assertEquals(staffId, app.lastModifiedBy)
    assertTrue(app.lastModifiedDate.isAfter(originalLastModifiedDate))
  }
}
