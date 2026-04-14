package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import com.fasterxml.uuid.Generators
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationGroup
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.SubmittedByType
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
  private val createdBy = "Staff12345"
  private val staffFullName = "Test Staff"
  private val groupId = Generators.timeBasedEpochGenerator().generate()
  private val forwardingComment = "Forwarding Comment"

  private lateinit var prisoner: Prisoner
  private lateinit var staff: Staff
  private lateinit var establishment: EstablishmentDto
  private lateinit var appRepository: AppRepository
  private lateinit var prisonerService: PrisonerService
  private lateinit var staffService: StaffService
  private lateinit var groupService: GroupService
  private lateinit var commentRepository: CommentRepository
  private lateinit var activityService: ActivityService
  private lateinit var historyService: HistoryService
  private lateinit var establishmentService: EstablishmentService
  private lateinit var applicationGroupRepository: ApplicationGroupRepository
  private lateinit var applicationTypeRepository: ApplicationTypeRepository

  private lateinit var appService: AppService
  private lateinit var app: App
  private lateinit var applicationGroup: ApplicationGroup
  private lateinit var applicationType: ApplicationType
  private lateinit var comment: Comment


  @BeforeEach
  fun setUp() {
    // TODO("Not yet implemented")
    appRepository = Mockito.mock(AppRepository::class.java)
    prisonerService = Mockito.mock(PrisonerService::class.java)
    groupService = Mockito.mock(GroupService::class.java)
    activityService = Mockito.mock(ActivityService::class.java)
    historyService = Mockito.mock(HistoryService::class.java)
    establishmentService = Mockito.mock(EstablishmentService::class.java)
    applicationTypeRepository = Mockito.mock(ApplicationTypeRepository::class.java)
    applicationGroupRepository = Mockito.mock(ApplicationGroupRepository::class.java)

    app = DataGenerator.generateApp(
      establishmentId,
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

    appService = AppServiceImplV2(
      appRepository,
      prisonerService,
      staffService,
      groupService,
      commentRepository,
      activityService,
      historyService,
      establishmentService,
      applicationGroupRepository,
      applicationTypeRepository,
    )
  }

  @AfterEach
  fun tearDown() {
    TODO("Not yet implemented")
  }

  @Test
  fun getAppsByPrisonerId() {
    Mockito.`when`(appRepository.findById(app.id)).thenReturn(Optional.of<App>(app))
    Mockito.`when`(prisonerService.getPrisonerById(prisoner.userId)).thenReturn(Optional.of<Prisoner>(prisoner))
    Mockito.`when`(establishmentService.getEstablishmentById(prisoner.establishmentId)).thenReturn(Optional.of<Establishment>(establishment))
  }

  @Test
  fun getPrisonerAppById() {
  }

  @Test
  fun submitApp() {
  }

}