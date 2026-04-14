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
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AssignedGroupDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationGroup
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Groups
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationGroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationTypeRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ESTABLISHMENT_ID_1
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
  private val groupId = Generators.timeBasedEpochGenerator().generate()

  private lateinit var prisoner: Prisoner
  private lateinit var establishment: EstablishmentDto
  private lateinit var appRepository: AppRepository
  private lateinit var prisonerService: PrisonerService
  private lateinit var groupService: GroupService
  private lateinit var activityService: ActivityService
  private lateinit var historyService: HistoryService
  private lateinit var establishmentService: EstablishmentService
  private lateinit var applicationGroupRepository: ApplicationGroupRepository
  private lateinit var applicationTypeRepository: ApplicationTypeRepository

  private lateinit var appService: AppPrisonerFacingService
  private lateinit var app: App
  private lateinit var applicationGroup: ApplicationGroup
  private lateinit var applicationType: ApplicationType


  @BeforeEach
  fun setUp() {
    appRepository = Mockito.mock(AppRepository::class.java)
    prisonerService = Mockito.mock(PrisonerService::class.java)
    groupService = Mockito.mock(GroupService::class.java)
    activityService = Mockito.mock(ActivityService::class.java)
    historyService = Mockito.mock(HistoryService::class.java)
    establishmentService = Mockito.mock(EstablishmentService::class.java)
    applicationTypeRepository = Mockito.mock(ApplicationTypeRepository::class.java)
    applicationGroupRepository = Mockito.mock(ApplicationGroupRepository::class.java)
    // app.submittedByType = SubmittedByType.PRISONER

    establishment =
      EstablishmentDto(ESTABLISHMENT_ID_1, "Test Establishment", AppType.entries.toSet(), false, setOf(), setOf())
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
      AppStatus.PENDING,
      groupId,
      false,
    )
    Mockito.`when`(groupService.getGroupByInitialAppType(prisoner.establishmentId!!, app.applicationType!!)).thenReturn(
      listOf(
        Groups(
          groupId,
          "Test Group",
          establishmentId,
          listOf(),
          listOf(1L),
          GroupType.WING,
        ),
      ),
    )
    Mockito.`when`(groupService.getGroupById(groupId, null)).thenReturn(
      AssignedGroupDto(
        groupId,
        "Test Group",
        EstablishmentDto(
          establishmentId,
          "Test Establishment",
          AppType.entries.toSet(),
          false,
          setOf(),
          setOf(),
        ),
        1L,
        GroupType.WING,
      ),
    )
    appService = AppPrisonerFacingService(
      appRepository,
      applicationTypeRepository,
      prisonerService,
      groupService,
      establishmentService,
      activityService,
    )
  }

  @AfterEach
  fun tearDown() {
    //   TODO("Not yet implemented")
  }

  @Test
  fun getAppsByPrisonerId() {
    Mockito.`when`(applicationTypeRepository.findById(1L)).thenReturn(Optional.of<ApplicationType>(applicationType))
    Mockito.`when`(
      appRepository.findAppsByRequestedBy(
        prisoner.username,
        PageRequest.of(0, 5).withSort(Sort.Direction.ASC, "createdDate"),
      ),
    )
      .thenReturn(
        PageImpl(
          listOf<App>(app),
          PageRequest.of(0, 5).withSort(Sort.Direction.ASC, "createdDate"),
          listOf<App>(app).size.toLong()
        ),
      )
    Mockito.`when`(prisonerService.getPrisonerById(prisoner.username)).thenReturn(Optional.of<Prisoner>(prisoner))
    Mockito.`when`(establishmentService.getEstablishmentById(prisoner.establishmentId!!))
      .thenReturn(Optional.of<EstablishmentDto>(establishment))
    val pApp = appService.getAppsByPrisonerId(prisoner.username, 1L, 5L)
    assertThat(pApp).isNotNull
  }

  @Test
  fun getPrisonerAppById() {
    Mockito.`when`(applicationTypeRepository.findById(1L)).thenReturn(Optional.of<ApplicationType>(applicationType))
    Mockito.`when`(appRepository.findById(app.id)).thenReturn(Optional.of<App>(app))
    Mockito.`when`(prisonerService.getPrisonerById(prisoner.username)).thenReturn(Optional.of<Prisoner>(prisoner))
    Mockito.`when`(establishmentService.getEstablishmentById(prisoner.establishmentId!!))
      .thenReturn(Optional.of<EstablishmentDto>(establishment))
    val pApps = appService.getPrisonerAppById(prisoner.username, app.id)
    assertThat(pApps).isNotNull
  }

  @Test
  fun submitApp() {
    Mockito.`when`(applicationTypeRepository.findById(1L)).thenReturn(Optional.of<ApplicationType>(applicationType))
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