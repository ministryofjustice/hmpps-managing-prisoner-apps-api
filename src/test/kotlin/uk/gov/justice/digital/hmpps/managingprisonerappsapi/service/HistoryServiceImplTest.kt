package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import com.fasterxml.uuid.Generators
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.CommentVisibility
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Decision
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Groups
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.History
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Response
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.StaffType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.HistoryRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ResponseRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class HistoryServiceImplTest {

  private val establishmentId = "TE1"
  private val requestedBy = "A12345"
  private val requestedByFirstName = "John"
  private val requestedByLastName = "Doe"
  private val staffId = "Staff12345"
  private val staffFullName = "Jane Smith"
  private val groupId: UUID = Generators.timeBasedEpochGenerator().generate()
  private val appId: UUID = Generators.timeBasedEpochGenerator().generate()
  private val createdDate: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)

  private lateinit var historyRepository: HistoryRepository
  private lateinit var staffService: StaffService
  private lateinit var groupService: GroupService
  private lateinit var appRepository: AppRepository
  private lateinit var commentRepository: CommentRepository
  private lateinit var responseRepository: ResponseRepository
  private lateinit var historyServiceImpl: HistoryServiceImpl

  private lateinit var staff: Staff
  private lateinit var group: Groups

  @BeforeEach
  fun setUp() {
    historyRepository = Mockito.mock(HistoryRepository::class.java)
    staffService = Mockito.mock(StaffService::class.java)
    groupService = Mockito.mock(GroupService::class.java)
    appRepository = Mockito.mock(AppRepository::class.java)
    commentRepository = Mockito.mock(CommentRepository::class.java)
    responseRepository = Mockito.mock(ResponseRepository::class.java)

    historyServiceImpl = HistoryServiceImpl(
      historyRepository,
      staffService,
      groupService,
      appRepository,
      commentRepository,
      responseRepository,
    )

    staff = Staff(
      staffId,
      UUID.randomUUID().toString(),
      staffFullName,
      UserCategory.STAFF,
      establishmentId,
      "Officer",
      UUID.randomUUID(),
    )

    group = Groups(
      groupId,
      "Test Group",
      establishmentId,
      emptyList(),
      GroupType.WING,
    )
  }

  private fun buildApp(requestedBy: String = this.requestedBy) = DataGenerator.generateApp(
    establishmentId,
    null,
    1L,
    1L,
    requestedBy,
    createdDate,
    requestedByFirstName,
    requestedByLastName,
    AppStatus.NEW,
    groupId,
    false,
  )

  private fun buildHistory(
    activity: Activity,
    entityType: EntityType,
    entityId: UUID = Generators.timeBasedEpochGenerator().generate(),
    createdBy: String = staffId,
    reference: String? = null,
  ) = History(
    Generators.timeBasedEpochGenerator().generate(),
    entityId,
    entityType,
    appId,
    activity,
    establishmentId,
    createdBy,
    createdDate,
    reference,
  )

  private fun setupHistoryAndApp(histories: List<History>, requestedBy: String = this.requestedBy) {
    val app = buildApp(requestedBy)
    Mockito.`when`(historyRepository.findByAppIdAndEstablishmentOrderByCreatedDate(appId, establishmentId))
      .thenReturn(histories)
    Mockito.`when`(appRepository.findById(appId)).thenReturn(Optional.of(app))
  }

  @Test
  fun `createdBy resolves to staff full name when history was created by a staff member`() {
    val history = buildHistory(Activity.APP_IN_PROGRESS, EntityType.APP, createdBy = staffId)
    setupHistoryAndApp(listOf(history))
    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(1, result.size)
    assertEquals(staffFullName, result[0].activityMessage.createdBy)
  }

  @Test
  fun `createdBy resolves to prisoner display name when history was created by the prisoner`() {
    val history = buildHistory(Activity.APP_IN_PROGRESS, EntityType.APP, createdBy = requestedBy)
    setupHistoryAndApp(listOf(history))

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(1, result.size)
    assertEquals("$requestedByFirstName $requestedByLastName [PRISONER]", result[0].activityMessage.createdBy)
  }

  @Test
  fun `createdBy resolves to MANAGE_APPS_ADMIN when history was created by the system admin`() {
    val history = buildHistory(Activity.APP_IN_PROGRESS, EntityType.APP, createdBy = StaffType.MANAGE_APPS_ADMIN.name)
    setupHistoryAndApp(listOf(history))

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(1, result.size)
    assertEquals(StaffType.MANAGE_APPS_ADMIN.name, result[0].activityMessage.createdBy)
  }

  @Test
  fun `throws ApiException when app not found`() {
    Mockito.`when`(historyRepository.findByAppIdAndEstablishmentOrderByCreatedDate(appId, establishmentId))
      .thenReturn(listOf(buildHistory(Activity.APP_IN_PROGRESS, EntityType.APP)))
    Mockito.`when`(appRepository.findById(appId)).thenReturn(Optional.empty())

    val exception = assertThrows(ApiException::class.java) {
      historyServiceImpl.getHistoryByAppId(appId, establishmentId)
    }
    assertEquals(HttpStatus.NOT_FOUND, exception.status)
  }

  @Test
  fun `throws ApiException when staff not found for history entry`() {
    val history = buildHistory(Activity.APP_IN_PROGRESS, EntityType.APP, createdBy = "unknownStaff")
    setupHistoryAndApp(listOf(history))
    Mockito.`when`(staffService.getStaffById("unknownStaff")).thenReturn(Optional.empty())

    val exception = assertThrows(ApiException::class.java) {
      historyServiceImpl.getHistoryByAppId(appId, establishmentId)
    }
    assertEquals(HttpStatus.NOT_FOUND, exception.status)
  }

  @Test
  fun `APP_SUBMITTED produces header 'Application logged' with group assignment body`() {
    val history = buildHistory(Activity.APP_SUBMITTED, EntityType.ASSIGNED_GROUP, entityId = groupId)
    setupHistoryAndApp(listOf(history))
    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))
    Mockito.`when`(groupService.getGroupById(groupId)).thenReturn(group)

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(1, result.size)
    assertEquals("Application logged", result[0].activityMessage.header)
    assertEquals("Assigned to ${group.name}", result[0].activityMessage.body)
  }

  @Test
  fun `APP_IN_PROGRESS produces header 'Application set to In Progress'`() {
    val history = buildHistory(Activity.APP_IN_PROGRESS, EntityType.APP)
    setupHistoryAndApp(listOf(history))
    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(1, result.size)
    assertEquals("Application set to In Progress", result[0].activityMessage.header)
  }

  @Test
  fun `PRISONER_ID_UPDATE produces header with merged prisoner reference`() {
    val mergedId = "B99999"
    val history = buildHistory(Activity.PRISONER_ID_UPDATE, EntityType.APP, reference = mergedId)
    setupHistoryAndApp(listOf(history))
    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(1, result.size)
    assertEquals("Prisoner Id merged with $mergedId", result[0].activityMessage.header)
  }

  @Test
  fun `APP_REQUEST_FORM_DATA_UPDATED produces header 'Form data updated'`() {
    val history = buildHistory(Activity.APP_REQUEST_FORM_DATA_UPDATED, EntityType.APP)
    setupHistoryAndApp(listOf(history))
    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(1, result.size)
    assertEquals("Form data updated", result[0].activityMessage.header)
  }

  @Test
  fun `APP_FORWARDED_TO_A_GROUP produces header 'Application forwarded' with group name`() {
    val history = buildHistory(Activity.APP_FORWARDED_TO_A_GROUP, EntityType.ASSIGNED_GROUP, entityId = groupId)
    setupHistoryAndApp(listOf(history))
    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))
    Mockito.`when`(groupService.getGroupById(groupId)).thenReturn(group)

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(1, result.size)
    assertEquals("Application forwarded", result[0].activityMessage.header)
    assertEquals("Forwarded to ${group.name}", result[0].activityMessage.body)
  }

  @Test
  fun `FORWARDING_COMMENT_ADDED with COMMENT entity type produces header 'Comment'`() {
    val commentId = Generators.timeBasedEpochGenerator().generate()
    val comment = Comment(commentId, "Forwarding note", createdDate, staffId, appId, CommentVisibility.STAFF_ONLY, UserCategory.STAFF)
    val history = buildHistory(Activity.FORWARDING_COMMENT_ADDED, EntityType.COMMENT, entityId = commentId)
    setupHistoryAndApp(listOf(history))
    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))
    Mockito.`when`(commentRepository.findById(commentId)).thenReturn(Optional.of(comment))

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(1, result.size)
    assertEquals("Comment", result[0].activityMessage.header)
    assertEquals(comment.message, result[0].activityMessage.body)
  }

  @Test
  fun `FORWARDING_COMMENT_ADDED is skipped when comment does not exist`() {
    val commentId = Generators.timeBasedEpochGenerator().generate()
    val history = buildHistory(Activity.FORWARDING_COMMENT_ADDED, EntityType.COMMENT, entityId = commentId)
    setupHistoryAndApp(listOf(history))
    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))
    Mockito.`when`(commentRepository.findById(commentId)).thenReturn(Optional.empty())

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(0, result.size)
  }

  @Test
  fun `COMMENT_ADDED with STAFF_ONLY visibility produces header 'Comment'`() {
    val commentId = Generators.timeBasedEpochGenerator().generate()
    val comment = Comment(commentId, "Staff-only note", createdDate, staffId, appId, CommentVisibility.STAFF_ONLY, UserCategory.STAFF)
    val history = buildHistory(Activity.COMMENT_ADDED, EntityType.COMMENT, entityId = commentId)
    setupHistoryAndApp(listOf(history))
    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))
    Mockito.`when`(commentRepository.findById(commentId)).thenReturn(Optional.of(comment))

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(1, result.size)
    assertEquals("Comment", result[0].activityMessage.header)
  }

  @Test
  fun `COMMENT_ADDED with STAFF_AND_PRISONER visibility and created by PRISONER produces header 'Message from prisoner'`() {
    val commentId = Generators.timeBasedEpochGenerator().generate()
    val comment = Comment(commentId, "Prisoner message", createdDate, requestedBy, appId, CommentVisibility.STAFF_AND_PRISONER, UserCategory.PRISONER)
    val history = buildHistory(Activity.COMMENT_ADDED, EntityType.COMMENT, entityId = commentId, createdBy = requestedBy)
    setupHistoryAndApp(listOf(history))
    Mockito.`when`(commentRepository.findById(commentId)).thenReturn(Optional.of(comment))

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(1, result.size)
    assertEquals("Message from prisoner", result[0].activityMessage.header)
  }

  @Test
  fun `COMMENT_ADDED with STAFF_AND_PRISONER visibility and created by STAFF produces header 'Message to prisoner'`() {
    val commentId = Generators.timeBasedEpochGenerator().generate()
    val comment = Comment(commentId, "Staff message to prisoner", createdDate, staffId, appId, CommentVisibility.STAFF_AND_PRISONER, UserCategory.STAFF)
    val history = buildHistory(Activity.COMMENT_ADDED, EntityType.COMMENT, entityId = commentId)
    setupHistoryAndApp(listOf(history))
    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))
    Mockito.`when`(commentRepository.findById(commentId)).thenReturn(Optional.of(comment))

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(1, result.size)
    assertEquals("Message to prisoner", result[0].activityMessage.header)
  }

  @Test
  fun `COMMENT_ADDED is skipped when comment does not exist`() {
    val commentId = Generators.timeBasedEpochGenerator().generate()
    val history = buildHistory(Activity.COMMENT_ADDED, EntityType.COMMENT, entityId = commentId)
    setupHistoryAndApp(listOf(history))
    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))
    Mockito.`when`(commentRepository.findById(commentId)).thenReturn(Optional.empty())

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(0, result.size)
  }

  @Test
  fun `APP_APPROVED with RESPONSE entity type produces header 'Application closed' and approved body`() {
    val responseId = Generators.timeBasedEpochGenerator().generate()
    val response = Response(responseId, "Meets all criteria", Decision.APPROVED, createdDate, staffId, appId)
    val history = buildHistory(Activity.APP_APPROVED, EntityType.RESPONSE, entityId = responseId)
    setupHistoryAndApp(listOf(history))
    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))
    Mockito.`when`(responseRepository.findById(responseId)).thenReturn(Optional.of(response))

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(1, result.size)
    assertEquals("Application closed.", result[0].activityMessage.header)
    assertEquals("Application approved. ${response.reason}", result[0].activityMessage.body)
  }

  @Test
  fun `APP_DECLINED with RESPONSE entity type produces header 'Application closed' and declined body`() {
    val responseId = Generators.timeBasedEpochGenerator().generate()
    val response = Response(responseId, "Does not qualify", Decision.DECLINED, createdDate, staffId, appId)
    val history = buildHistory(Activity.APP_DECLINED, EntityType.RESPONSE, entityId = responseId)
    setupHistoryAndApp(listOf(history))
    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))
    Mockito.`when`(responseRepository.findById(responseId)).thenReturn(Optional.of(response))

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(1, result.size)
    assertEquals("Application closed.", result[0].activityMessage.header)
    assertEquals("Application declined. ${response.reason}", result[0].activityMessage.body)
  }

  @Test
  fun `APP_REJECTED with RESPONSE entity type produces header 'Application closed' and rejected body`() {
    val responseId = Generators.timeBasedEpochGenerator().generate()
    val response = Response(responseId, "Invalid request", Decision.REJECTED, createdDate, staffId, appId)
    val history = buildHistory(Activity.APP_REJECTED, EntityType.RESPONSE, entityId = responseId)
    setupHistoryAndApp(listOf(history))
    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))
    Mockito.`when`(responseRepository.findById(responseId)).thenReturn(Optional.of(response))

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(1, result.size)
    assertEquals("Application closed.", result[0].activityMessage.header)
    assertEquals("Application rejected. ${response.reason}", result[0].activityMessage.body)
  }

  @Test
  fun `APP_APPROVED with RESPONSE entity type and no response record produces header 'Application closed' and empty prefix`() {
    val responseId = Generators.timeBasedEpochGenerator().generate()
    val history = buildHistory(Activity.APP_APPROVED, EntityType.RESPONSE, entityId = responseId)
    setupHistoryAndApp(listOf(history))
    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))
    Mockito.`when`(responseRepository.findById(responseId)).thenReturn(Optional.empty())

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(1, result.size)
    assertEquals("Application closed.", result[0].activityMessage.header)
    assertEquals("Application approved. ", result[0].activityMessage.body)
  }

  @Test
  fun `returns empty list when no history exists for the app`() {
    val app = buildApp()
    Mockito.`when`(historyRepository.findByAppIdAndEstablishmentOrderByCreatedDate(appId, establishmentId))
      .thenReturn(emptyList())
    Mockito.`when`(appRepository.findById(appId)).thenReturn(Optional.of(app))

    val result = historyServiceImpl.getHistoryByAppId(appId, establishmentId)

    assertEquals(0, result.size)
  }
}
