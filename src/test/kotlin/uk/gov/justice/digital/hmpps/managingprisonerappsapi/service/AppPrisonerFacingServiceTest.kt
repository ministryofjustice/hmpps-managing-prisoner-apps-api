package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import com.fasterxml.uuid.Generators
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppRequestPrisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrisonerAppRow
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppScope
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationGroup
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Department
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EstablishmentApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationGroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationTypeRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ESTABLISHMENT_ID_1
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ResponseRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.stats.StatsTelemetryService
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class AppPrisonerFacingServiceTest {

  private val establishmentId = "TE1"
  private val requestedBy = "A12345"
  private val requestedByFirstName = "Test"
  private val requestedByLastName = "Prisoner"
  private val department = Department(Generators.timeBasedEpochGenerator().generate(), "test-department", "Test Department")

  private lateinit var prisoner: Prisoner
  private lateinit var establishment: EstablishmentDto
  private lateinit var appRepository: AppRepository
  private lateinit var prisonerService: PrisonerService
  private lateinit var establishmentDepartmentService: EstablishmentDepartmentService
  private lateinit var staffService: StaffService
  private lateinit var activityService: ActivityService
  private lateinit var historyService: HistoryService
  private lateinit var establishmentService: EstablishmentService
  private lateinit var statsTelemetryService: StatsTelemetryService
  private lateinit var applicationGroupRepository: ApplicationGroupRepository
  private lateinit var applicationTypeRepository: ApplicationTypeRepository
  private lateinit var responseRepository: ResponseRepository
  private lateinit var groupRepository: GroupRepository
  private lateinit var establishmentApplicationTypeService: EstablishmentApplicationTypeService

  private lateinit var appService: AppPrisonerFacingService
  private lateinit var app: App
  private lateinit var applicationGroup: ApplicationGroup
  private lateinit var applicationType: ApplicationType
  private val assignedGroup = UUID.randomUUID()

  @BeforeEach
  fun setUp() {
    appRepository = Mockito.mock(AppRepository::class.java)
    prisonerService = Mockito.mock(PrisonerService::class.java)
    establishmentDepartmentService = Mockito.mock(EstablishmentDepartmentService::class.java)
    Mockito.`when`(establishmentDepartmentService.findBySlugIgnoreCase(department.slug)).thenReturn(department)
    activityService = Mockito.mock(ActivityService::class.java)
    historyService = Mockito.mock(HistoryService::class.java)
    establishmentService = Mockito.mock(EstablishmentService::class.java)
    statsTelemetryService = Mockito.mock(StatsTelemetryService::class.java)
    applicationTypeRepository = Mockito.mock(ApplicationTypeRepository::class.java)
    applicationGroupRepository = Mockito.mock(ApplicationGroupRepository::class.java)
    responseRepository = Mockito.mock(ResponseRepository::class.java)
    groupRepository = Mockito.mock(GroupRepository::class.java)
    staffService = Mockito.mock(StaffService::class.java)
    establishmentApplicationTypeService = Mockito.mock(EstablishmentApplicationTypeService::class.java)
    val activeAppTypeView = Mockito.mock(uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EstablishmentApplicationTypeView::class.java)
    Mockito.`when`(activeAppTypeView.active).thenReturn(true)
    Mockito.`when`(activeAppTypeView.applicationGroupId).thenReturn(1L)
    Mockito.`when`(activeAppTypeView.applicationTypeId).thenReturn(1L)
    Mockito.`when`(establishmentApplicationTypeService.findApplicationTypeForEstablishment(any(), any())).thenReturn(activeAppTypeView)
    // app.submittedByType = SubmittedByType.PRISONER

    establishment =
      EstablishmentDto(ESTABLISHMENT_ID_1, "Test Establishment", false)
    prisoner = Prisoner(
      requestedBy,
      UUID.randomUUID().toString(),
      requestedByFirstName,
      requestedByLastName,
      UserCategory.PRISONER,
      "7-1-007",
      UUID.randomUUID().toString(),
      UUID.randomUUID().toString(),
      establishmentId,
      2,
    )

    applicationType = ApplicationType(1, "Add social contact", false, false, false)

    applicationGroup = ApplicationGroup(1, "Bt Pin Phones", listOf(applicationType))
    applicationType.applicationGroup = applicationGroup
    app = DataGenerator.generateApp(
      prisoner.establishmentId!!,
      null,
      1,
      1,
      requestedBy,
      LocalDateTime.now(ZoneOffset.UTC),
      requestedByFirstName,
      requestedByLastName,
      AppStatus.NEW,
      department.slug,
      false,
    )
    appService = AppPrisonerFacingService(
      appRepository,
      responseRepository,
      applicationTypeRepository,
      applicationGroupRepository,
      prisonerService,
      establishmentService,
      activityService,
      statsTelemetryService,
      establishmentDepartmentService,
      establishmentApplicationTypeService,
    )
  }

  @AfterEach
  fun tearDown() {
    //   TODO("Not yet implemented")
  }

  @Test
  fun `getAppsByPrisonerId with OPEN scope`() {
    Mockito.`when`(
      appRepository.findAppsForPrisoner(
        prisoner.username,
        setOf(AppStatus.NEW, AppStatus.IN_PROGRESS),
        PageRequest.of(0, 5).withSort(Sort.Direction.DESC, "createdDate"),
      ),
    ).thenReturn(
      PageImpl(
        listOf(PrisonerAppRow(app, applicationType.name, 2L)),
        PageRequest.of(0, 5).withSort(Sort.Direction.DESC, "createdDate"),
        1,
      ),
    )
    Mockito.`when`(prisonerService.getPrisonerById(prisoner.username)).thenReturn(Optional.of<Prisoner>(prisoner))
    Mockito.`when`(establishmentService.getEstablishmentById(prisoner.establishmentId!!))
      .thenReturn(Optional.of<EstablishmentDto>(establishment))
    val pApp = appService.getAppsByPrisonerId(prisoner.username, AppScope.OPEN, 1L, 5L)
    assertThat(pApp).isNotNull
    assertThat(pApp.apps).hasSize(1)
    assertThat(pApp.apps[0].applicationType).isEqualTo(applicationType.name)
    assertThat(pApp.apps[0].messageCount).isEqualTo(2L)
  }

  @Test
  fun `getAppsByPrisonerId with CLOSED scope`() {
    val closedApp = app.copy(status = AppStatus.APPROVED)
    Mockito.`when`(
      appRepository.findAppsForPrisoner(
        prisoner.username,
        setOf(AppStatus.APPROVED, AppStatus.DECLINED, AppStatus.REJECTED),
        PageRequest.of(0, 5).withSort(Sort.Direction.DESC, "lastModifiedDate"),
      ),
    ).thenReturn(
      PageImpl(
        listOf(PrisonerAppRow(closedApp, applicationType.name, 1L)),
        PageRequest.of(0, 5).withSort(Sort.Direction.DESC, "lastModifiedDate"),
        1,
      ),
    )
    Mockito.`when`(prisonerService.getPrisonerById(prisoner.username)).thenReturn(Optional.of<Prisoner>(prisoner))
    Mockito.`when`(establishmentService.getEstablishmentById(prisoner.establishmentId!!))
      .thenReturn(Optional.of<EstablishmentDto>(establishment))

    val pApp = appService.getAppsByPrisonerId(prisoner.username, AppScope.CLOSED, 1L, 5L)
    assertThat(pApp).isNotNull
    assertThat(pApp.apps).hasSize(1)
    assertThat(pApp.apps[0].status).isEqualTo(AppStatus.APPROVED)
    assertThat(pApp.apps[0].messageCount).isEqualTo(1L)
  }

  @Test
  fun getPrisonerAppById() {
    val prisonerEstablishmentId = prisoner.establishmentId!!
    Mockito.`when`(applicationTypeRepository.findById(1L)).thenReturn(Optional.of<ApplicationType>(applicationType))
    Mockito.`when`(appRepository.findById(app.id)).thenReturn(Optional.of<App>(app))
    Mockito.`when`(prisonerService.getPrisonerById(prisoner.username)).thenReturn(Optional.of<Prisoner>(prisoner))
    Mockito.`when`(establishmentService.getEstablishmentById(prisonerEstablishmentId))
      .thenReturn(Optional.of<EstablishmentDto>(establishment))
    Mockito.`when`(establishmentApplicationTypeService.findDefaultApplicationTypeForEstablishment(prisonerEstablishmentId, app.applicationType!!)).thenReturn(
      EstablishmentApplicationType(
        id = UUID.randomUUID(),
        establishment = Establishment(prisonerEstablishmentId, "Test Establishment", false),
        applicationType = applicationType,
        active = true,
        departmentSlug = department.slug,
      ),
    )
    val pApps = appService.getPrisonerAppById(prisoner.username, app.id)
    assertThat(pApps).isNotNull
    assertThat(pApps.id).isEqualTo(app.id)
    assertThat(pApps.applicationType.name).isEqualTo(applicationType.name)
    assertThat(pApps.applicationGroup.name).isEqualTo(applicationGroup.name)
    assertThat(pApps.requestedByFirstName).isEqualTo(prisoner.firstName)
    assertThat(pApps.requestedByLastName).isEqualTo(prisoner.lastName)
    assertThat(pApps.status).isEqualTo(AppStatus.NEW)
    assertThat(pApps.establishmentId).isEqualTo(prisonerEstablishmentId)
    assertThat(pApps.reason).isNull()
  }

  @Test
  fun submitApp() {
    Mockito.`when`(applicationTypeRepository.findById(1L)).thenReturn(Optional.of(applicationType))
    Mockito.`when`(applicationGroupRepository.findById(1L)).thenReturn(Optional.of(applicationGroup))
    Mockito.`when`(establishmentApplicationTypeService.findDefaultApplicationTypeForEstablishment(establishmentId, 1L)).thenReturn(
      EstablishmentApplicationType(
        id = UUID.randomUUID(),
        establishment = Establishment(establishmentId, "Test Establishment", false),
        applicationType = applicationType,
        active = true,
        departmentSlug = department.slug,
      ),
    )
    Mockito.`when`(appRepository.save(any())).thenReturn(app)
    Mockito.`when`(prisonerService.getPrisonerById(prisoner.username)).thenReturn(Optional.of<Prisoner>(prisoner))
    Mockito.`when`(establishmentService.getEstablishmentById(prisoner.establishmentId!!))
      .thenReturn(Optional.of<EstablishmentDto>(establishment))
    val pApp = appService.submitApp(
      AppRequestPrisoner(
        null,
        1L,
        true,
        app.requests,
      ),
      prisoner.username,
    )
    assertThat(pApp).isNotNull
  }
}
