package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import com.fasterxml.uuid.Generators
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AssignedGroupDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.CommentResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrisonerDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.StaffDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationGroup
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.CommentVisibility
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationGroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationTypeRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.stats.StatsTelemetryService
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class CommentServiceImplTest {

  private val establishmentId = "TE1"
  private val requestedBy = "A12345"
  private val requestedByFirstName = "Test"
  private val requestedByLastName = "Prisoner"
  private val createdBy = "Staff12345"
  private val staffFullName = "Test Staff"
  private val groupId = Generators.timeBasedEpochGenerator().generate()
  private val message = "This is require more information from other department"
  private val createdDate = LocalDateTime.now(ZoneOffset.UTC)

  private lateinit var staff: Staff
  private lateinit var prisoner: Prisoner
  private lateinit var appService: AppService
  private lateinit var staffService: StaffService
  private lateinit var prisonerService: PrisonerService
  private lateinit var commentRepository: CommentRepository
  private lateinit var establishmentService: EstablishmentService
  private lateinit var commentServiceImpl: CommentServiceImpl
  private lateinit var activityService: ActivityService
  private lateinit var app: App
  private lateinit var commentByStaff: Comment
  private lateinit var commentByPrisoner: Comment
  private lateinit var groupService: GroupService
  private lateinit var applicationGroupRepository: ApplicationGroupRepository
  private lateinit var applicationTypeRepository: ApplicationTypeRepository
  private lateinit var statsTelemetryService: StatsTelemetryService
  private lateinit var applicationType: ApplicationType
  private lateinit var applicationGroup: ApplicationGroup

  @BeforeEach
  fun beforeEach() {
    app = DataGenerator.generateApp(
      establishmentId,
      null,
      1,
      1,
      requestedBy,
      LocalDateTime.now(ZoneOffset.UTC),
      requestedByFirstName,
      requestedByLastName,
      AppStatus.NEW,
      groupId,
      false,
    )

    staff = Staff(
      createdBy,
      UUID.randomUUID().toString(),
      staffFullName,
      UserCategory.STAFF,
      establishmentId,
      "Staff",
      UUID.randomUUID(),
    )

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

    commentByStaff = Comment(
      Generators.timeBasedEpochGenerator().generate(),
      message,
      createdDate,
      createdBy,
      app.id,
      CommentVisibility.STAFF_ONLY,
      UserCategory.STAFF,
    )
    commentByPrisoner = Comment(
      Generators.timeBasedEpochGenerator().generate(),
      message,
      createdDate,
      requestedBy,
      app.id,
      CommentVisibility.STAFF_AND_PRISONER,
      UserCategory.PRISONER,
    )
    appService = Mockito.mock(AppService::class.java)
    commentRepository = Mockito.mock(CommentRepository::class.java)
    establishmentService = Mockito.mock(EstablishmentService::class.java)
    staffService = Mockito.mock(StaffService::class.java)
    prisonerService = Mockito.mock(PrisonerService::class.java)
    activityService = Mockito.mock(ActivityService::class.java)
    groupService = Mockito.mock(GroupService::class.java)
    applicationGroupRepository = Mockito.mock(ApplicationGroupRepository::class.java)
    applicationTypeRepository = Mockito.mock(ApplicationTypeRepository::class.java)
    statsTelemetryService = Mockito.mock(StatsTelemetryService::class.java)
    applicationType = ApplicationType(1, "Add social contact", false, false, false)
    applicationGroup = ApplicationGroup(1, "PIN phones", listOf(applicationType))

    commentServiceImpl = CommentServiceImpl(
      staffService,
      appService,
      commentRepository,
      establishmentService,
      activityService,
      prisonerService,
      groupService,
      applicationTypeRepository,
      applicationGroupRepository,
      statsTelemetryService,
    )
  }

  @AfterEach
  fun afterEach() {
  }

  @Test
  fun `add comment by staff`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(Optional.of(staff))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(applicationTypeRepository.findById(1L)).thenReturn(Optional.of<ApplicationType>(applicationType))
    Mockito.`when`(applicationGroupRepository.findById(1)).thenReturn(Optional.of(applicationGroup))
    Mockito.`when`(groupService.getGroupById(app.assignedGroup, staff.establishmentId)).thenReturn(
      AssignedGroupDto(
        groupId,
        "Test Group",
        EstablishmentDto(establishmentId, "Test Establishment", false, setOf(), setOf()),
        1L,
        GroupType.WING,
      ),
    )
    Mockito.`when`(commentRepository.save(any())).thenReturn(commentByStaff)
    val result = commentServiceImpl.addCommentByStaff(
      requestedBy,
      createdBy,
      app.id,
      message,
    )
    assertComment(commentByStaff, result, false, requestedBy)
  }

  @Test
  fun `add comment by prisoner`() {
    Mockito.`when`(prisonerService.getPrisonerById(requestedBy))
      .thenReturn(Optional.of(prisoner))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(applicationTypeRepository.findById(1L)).thenReturn(Optional.of<ApplicationType>(applicationType))
    Mockito.`when`(applicationGroupRepository.findById(1)).thenReturn(Optional.of(applicationGroup))
    Mockito.`when`(groupService.getGroupById(app.assignedGroup, prisoner.establishmentId!!)).thenReturn(
      AssignedGroupDto(
        groupId,
        "Test Group",
        EstablishmentDto(establishmentId, "Test Establishment", false, setOf(), setOf()),
        1L,
        GroupType.WING,
      ),
    )
    Mockito.`when`(commentRepository.save(any())).thenReturn(commentByPrisoner)
    val result = commentServiceImpl.addMessageByPrisoner(
      requestedBy,
      app.id,
      message,
    )
    assertComment(commentByPrisoner, result, false, requestedBy)
  }

  @Test
  fun `add comment when staff do not found`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(Optional.empty())
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(commentRepository.save(any())).thenReturn(commentByStaff)
    val exception = assertThrows(ApiException::class.java) {
      commentServiceImpl.addCommentByStaff(
        requestedBy,
        createdBy,
        app.id,
        message,
      )
    }
    assertEquals(HttpStatus.NOT_FOUND, exception.status)
  }

  @Test
  fun `add comment when staff establishment is different from app establishment value`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(
        Optional.of(
          Staff(
            createdBy,
            UUID.randomUUID().toString(),
            staffFullName,
            UserCategory.STAFF,
            "random_establishment",
            "Staff",
            UUID.randomUUID(),
          ),
        ),
      )
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(commentRepository.save(any())).thenReturn(commentByStaff)
    val exception = assertThrows(ApiException::class.java) {
      commentServiceImpl.addCommentByStaff(
        requestedBy,
        createdBy,
        app.id,
        message,
      )
    }
    assertEquals(HttpStatus.FORBIDDEN, exception.status)
  }

  @Test
  fun `add comment when app not found`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(Optional.of(staff))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.empty())
    Mockito.`when`(commentRepository.save(any())).thenReturn(commentByStaff)
    val exception = assertThrows(ApiException::class.java) {
      commentServiceImpl.addCommentByStaff(
        requestedBy,
        createdBy,
        app.id,
        message,
      )
    }
    assertEquals(HttpStatus.FORBIDDEN, exception.status)
  }

  @Test
  fun `add comment when app requested by is different than prisoner id provided`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(Optional.of(staff))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(commentRepository.save(any())).thenReturn(commentByStaff)
    val exception = assertThrows(ApiException::class.java) {
      commentServiceImpl.addCommentByStaff(
        "X12345",
        createdBy,
        app.id,
        message,
      )
    }
    assertEquals(HttpStatus.FORBIDDEN, exception.status)
  }

  @Test
  fun `get comment by id by staff when created by false`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(Optional.of(staff))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(commentRepository.findById(commentByStaff.id)).thenReturn(Optional.of(commentByStaff))
    val result = commentServiceImpl.getCommentByIdForStaff(requestedBy, createdBy, app.id, false, commentByStaff.id)
    assertEquals(app.requestedBy, result.prisonerNumber)
    assertComment(commentByStaff, result, false, requestedBy)
  }

  @Test
  fun `get comment by id by prisoner when created by false`() {
    Mockito.`when`(prisonerService.getPrisonerById(requestedBy))
      .thenReturn(Optional.of(prisoner))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(commentRepository.findById(commentByPrisoner.id)).thenReturn(Optional.of(commentByPrisoner))
    val result = commentServiceImpl.getCommentByIdForPrisoner(requestedBy, app.id, false, commentByPrisoner.id)
    assertEquals(app.requestedBy, result.prisonerNumber)
    assertComment(commentByPrisoner, result, false, requestedBy)
  }

  @Test
  fun `get comment by id by staff  when created by true`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(Optional.of(staff))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(commentRepository.findById(commentByStaff.id)).thenReturn(Optional.of(commentByStaff))
    Mockito.`when`(establishmentService.getEstablishmentById(establishmentId)).thenReturn(
      Optional.of(
        EstablishmentDto(establishmentId, "Test Establishment", false, setOf(), setOf()),
      ),
    )
    val result = commentServiceImpl.getCommentByIdForStaff(requestedBy, createdBy, app.id, true, commentByStaff.id)
    assertEquals(app.requestedBy, result.prisonerNumber)
    assertComment(commentByStaff, result, true, requestedBy)
  }

  @Test
  fun `get comment by id by prisoner  when created by true`() {
    Mockito.`when`(prisonerService.getPrisonerById(requestedBy))
      .thenReturn(Optional.of(prisoner))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(commentRepository.findById(commentByPrisoner.id)).thenReturn(Optional.of(commentByPrisoner))
    Mockito.`when`(establishmentService.getEstablishmentById(establishmentId)).thenReturn(
      Optional.of(
        EstablishmentDto(establishmentId, "Test Establishment", false, setOf(), setOf()),
      ),
    )
    val result = commentServiceImpl.getCommentByIdForPrisoner(requestedBy, app.id, true, commentByPrisoner.id)
    assertEquals(app.requestedBy, result.prisonerNumber)
    assertComment(commentByPrisoner, result, true, requestedBy)
  }

  @Test
  fun `get comment by id when created by true and establishment of created by staff do not exist`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(Optional.of(staff))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(commentRepository.findById(commentByStaff.id)).thenReturn(Optional.of(commentByStaff))
    Mockito.`when`(establishmentService.getEstablishmentById(establishmentId)).thenReturn(Optional.empty())
    val exception = assertThrows(ApiException::class.java) {
      commentServiceImpl.getCommentByIdForStaff(requestedBy, createdBy, app.id, true, commentByStaff.id)
    }
    assertEquals(HttpStatus.NOT_FOUND, exception.status)
  }

  @Test
  fun `get comments by app id staff when created by false`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(Optional.of(staff))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(establishmentService.getEstablishmentById(establishmentId)).thenReturn(
      Optional.of(
        EstablishmentDto(establishmentId, "Test Establishment", false, setOf(), setOf()),
      ),
    )
    Mockito.`when`(commentRepository.getCommentsByAppIdAndVisibility(app.id, CommentVisibility.STAFF_ONLY, PageRequest.of(0, 5).withSort(Sort.by(Sort.Direction.ASC, "createdDate"))))
      .thenReturn(PageImpl(listOf(commentByStaff), PageRequest.of(0, 5), 1))
    val result = commentServiceImpl.getCommentsByAppIdForStaff(requestedBy, createdBy, app.id, 1, 5)
    assertEquals(1, result.totalElements)
    assertEquals(true, result.exhausted)
    assertComment(commentByStaff, result.contents.get(0), true, requestedBy)
  }

  @Test
  fun `get comments by app id prisoner when created by false`() {
    Mockito.`when`(prisonerService.getPrisonerById(requestedBy))
      .thenReturn(Optional.of(prisoner))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(establishmentService.getEstablishmentById(establishmentId)).thenReturn(
      Optional.of(
        EstablishmentDto(establishmentId, "Test Establishment", false, setOf(), setOf()),
      ),
    )
    Mockito.`when`(commentRepository.getCommentsByAppIdAndVisibility(app.id, CommentVisibility.STAFF_AND_PRISONER, PageRequest.of(0, 5).withSort(Sort.by(Sort.Direction.ASC, "createdDate"))))
      .thenReturn(PageImpl(listOf(commentByPrisoner), PageRequest.of(0, 5), 1))
    val result = commentServiceImpl.getMessagesByAppIdForPrisoner(requestedBy, app.id, 1, 5)
    assertEquals(1, result.totalElements)
    assertEquals(true, result.exhausted)
    assertComment(commentByPrisoner, result.contents.get(0), true, requestedBy)
  }

  @Test
  fun `get comments by app id staff when created by true`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(Optional.of(staff))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(establishmentService.getEstablishmentById(establishmentId)).thenReturn(
      Optional.of(
        EstablishmentDto(establishmentId, "Test Establishment", false, setOf(), setOf()),
      ),
    )
    Mockito.`when`(commentRepository.getCommentsByAppIdAndVisibility(app.id, CommentVisibility.STAFF_ONLY, PageRequest.of(0, 5).withSort(Sort.by(Sort.Direction.ASC, "createdDate"))))
      .thenReturn(PageImpl(listOf(commentByStaff), PageRequest.of(0, 5), 1))
    val result = commentServiceImpl.getCommentsByAppIdForStaff(requestedBy, createdBy, app.id, 1, 5)
    assertEquals(1, result.totalElements)
    assertEquals(true, result.exhausted)
    assertComment(commentByStaff, result.contents.get(0), true, requestedBy)
  }

  @Test
  fun `get comments by app id prisoner when created by true`() {
    Mockito.`when`(prisonerService.getPrisonerById(requestedBy))
      .thenReturn(Optional.of(prisoner))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(establishmentService.getEstablishmentById(establishmentId)).thenReturn(
      Optional.of(
        EstablishmentDto(establishmentId, "Test Establishment", false, setOf(), setOf()),
      ),
    )
    Mockito.`when`(commentRepository.getCommentsByAppIdAndVisibility(app.id, CommentVisibility.STAFF_AND_PRISONER, PageRequest.of(0, 5).withSort(Sort.by(Sort.Direction.ASC, "createdDate"))))
      .thenReturn(PageImpl(listOf(commentByPrisoner), PageRequest.of(0, 5), 1))
    val result = commentServiceImpl.getMessagesByAppIdForPrisoner(requestedBy, app.id, 1, 5)
    assertEquals(1, result.totalElements)
    assertEquals(true, result.exhausted)
    assertComment(commentByPrisoner, result.contents.get(0), true, requestedBy)
  }

  private fun assertComment(
    expected: Comment,
    result: CommentResponseDto<Any>,
    createdBy: Boolean,
    prisonerNumber: String,
  ) {
    assertEquals(expected.id, result.id)
    assertEquals(expected.message, result.message)
    if (createdBy && expected.createdByUserType == UserCategory.STAFF) {
      assertInstanceOf(StaffDto::class.java, result.createdBy)
      assertEquals(expected.createdBy, (result.createdBy as StaffDto).username)
    } else if (createdBy && expected.createdByUserType == UserCategory.PRISONER) {
      assertInstanceOf(PrisonerDto::class.java, result.createdBy)
      assertEquals(expected.createdBy, (result.createdBy as PrisonerDto).username)
    } else {
      assertEquals(expected.createdBy, result.createdBy)
    }
    assertEquals(prisonerNumber, result.prisonerNumber)
    assertEquals(expected.appId, result.appId)
    assertEquals(expected.createdDate, result.createdDate)
    assertEquals(expected.createdByUserType, result.createdByType)
  }
}
