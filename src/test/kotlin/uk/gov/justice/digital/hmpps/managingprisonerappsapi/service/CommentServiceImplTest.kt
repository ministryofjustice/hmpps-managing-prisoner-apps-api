package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

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
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.CommentRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.CommentResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.StaffDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
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
  private val groupId = UUID.randomUUID()
  private val message = "This is require more information from other department"
  private val createdDate = LocalDateTime.now(ZoneOffset.UTC)

  private lateinit var staff: Staff
  private lateinit var appService: AppService
  private lateinit var staffService: StaffService
  private lateinit var commentRepository: CommentRepository
  private lateinit var establishmentService: EstablishmentService
  private lateinit var commentServiceImpl: CommentServiceImpl
  private lateinit var app: App
  private lateinit var comment: Comment

  @BeforeEach
  fun beforeEach() {
    app = DataGenerator.generateApp(
      establishmentId,
      AppType.PIN_PHONE_ADD_NEW_CONTACT,
      requestedBy,
      LocalDateTime.now(ZoneOffset.UTC),
      requestedByFirstName,
      requestedByLastName,
      AppStatus.PENDING,
      groupId,
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

    comment = Comment(UUID.randomUUID(), message, createdDate, createdBy, app.id)
    appService = Mockito.mock(AppService::class.java)
    commentRepository = Mockito.mock(CommentRepository::class.java)
    establishmentService = Mockito.mock(EstablishmentService::class.java)
    staffService = Mockito.mock(StaffService::class.java)
    commentServiceImpl = CommentServiceImpl(staffService, appService, commentRepository, establishmentService)
  }

  @AfterEach
  fun afterEach() {
  }

  @Test
  fun `add comment`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(Optional.of(staff))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(commentRepository.save(any())).thenReturn(comment)
    val result = commentServiceImpl.addComment(requestedBy, createdBy, app.id, CommentRequestDto(message))
    assertComment(comment, result, false, requestedBy)
  }

  @Test
  fun `add comment when staff do not found`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(Optional.empty())
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(commentRepository.save(any())).thenReturn(comment)
    val exception = assertThrows(ApiException::class.java) {
      commentServiceImpl.addComment(requestedBy, createdBy, app.id, CommentRequestDto(message))
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
    Mockito.`when`(commentRepository.save(any())).thenReturn(comment)
    val exception = assertThrows(ApiException::class.java) {
      commentServiceImpl.addComment(requestedBy, createdBy, app.id, CommentRequestDto(message))
    }
    assertEquals(HttpStatus.FORBIDDEN, exception.status)
  }

  @Test
  fun `add comment when app not found`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(Optional.of(staff))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.empty())
    Mockito.`when`(commentRepository.save(any())).thenReturn(comment)
    val exception = assertThrows(ApiException::class.java) {
      commentServiceImpl.addComment(requestedBy, createdBy, app.id, CommentRequestDto(message))
    }
    assertEquals(HttpStatus.FORBIDDEN, exception.status)
  }

  @Test
  fun `add comment when app requested by is different than prisoner id provided`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(Optional.of(staff))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(commentRepository.save(any())).thenReturn(comment)
    val exception = assertThrows(ApiException::class.java) {
      commentServiceImpl.addComment("X12345", createdBy, app.id, CommentRequestDto(message))
    }
    assertEquals(HttpStatus.FORBIDDEN, exception.status)
  }

  @Test
  fun `get comment by id when created by false`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(Optional.of(staff))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(commentRepository.findById(comment.id)).thenReturn(Optional.of(comment))
    val result = commentServiceImpl.getCommentById(requestedBy, createdBy, app.id, false, comment.id)
    assertEquals(app.requestedBy, result.prisonerNumber)
    assertComment(comment, result, false, requestedBy)
  }

  @Test
  fun `get comment by id when created by true`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(Optional.of(staff))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(commentRepository.findById(comment.id)).thenReturn(Optional.of(comment))
    Mockito.`when`(establishmentService.getEstablishmentById(establishmentId)).thenReturn(
      Optional.of(
        EstablishmentDto(establishmentId, "Test Establishment"),
      ),
    )
    val result = commentServiceImpl.getCommentById(requestedBy, createdBy, app.id, true, comment.id)
    assertEquals(app.requestedBy, result.prisonerNumber)
    assertComment(comment, result, true, requestedBy)
  }

  @Test
  fun `get comment by id when created by true and establishment of created by staff do not exist`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(Optional.of(staff))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(commentRepository.findById(comment.id)).thenReturn(Optional.of(comment))
    Mockito.`when`(establishmentService.getEstablishmentById(establishmentId)).thenReturn(Optional.empty())
    val exception = assertThrows(ApiException::class.java) {
      commentServiceImpl.getCommentById(requestedBy, createdBy, app.id, true, comment.id)
    }
    assertEquals(HttpStatus.NOT_FOUND, exception.status)
  }

  @Test
  fun `get comments by app id when created by false`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(Optional.of(staff))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(commentRepository.getCommentsByAppId(app.id, PageRequest.of(0, 5)))
      .thenReturn(PageImpl(listOf(comment), PageRequest.of(0, 5), 1))
    val result = commentServiceImpl.getCommentsByAppId(requestedBy, createdBy, app.id, false, 1, 5)
    assertEquals(1, result.totalElements)
    assertEquals(true, result.exhausted)
    assertComment(comment, result.contents.get(0), false, requestedBy)
  }

  @Test
  fun `get comments by app id when created by true`() {
    Mockito.`when`(staffService.getStaffById(createdBy))
      .thenReturn(Optional.of(staff))
    Mockito.`when`(appService.getAppById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(establishmentService.getEstablishmentById(establishmentId)).thenReturn(
      Optional.of(
        EstablishmentDto(establishmentId, "Test Establishment"),
      ),
    )
    Mockito.`when`(commentRepository.getCommentsByAppId(app.id, PageRequest.of(0, 5)))
      .thenReturn(PageImpl(listOf(comment), PageRequest.of(0, 5), 1))
    val result = commentServiceImpl.getCommentsByAppId(requestedBy, createdBy, app.id, true, 1, 5)
    assertEquals(1, result.totalElements)
    assertEquals(true, result.exhausted)
    assertComment(comment, result.contents.get(0), true, requestedBy)
  }

  private fun assertComment(
    expected: Comment,
    result: CommentResponseDto<Any>,
    createdBy: Boolean,
    prisonerNumber: String,
  ) {
    assertEquals(expected.id, result.id)
    assertEquals(expected.message, result.message)
    if (createdBy) {
      assertInstanceOf(StaffDto::class.java, result.createdBy)
    } else {
      assertEquals(expected.createdBy, result.createdBy)
    }
    assertEquals(prisonerNumber, result.prisonerNumber)
    assertEquals(expected.appId, result.appId)
    assertEquals(expected.createdDate, result.createdDate)
  }
}
