package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.CommentRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.CommentResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.PageResultComments
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.StaffDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.collections.ArrayList

@Service
class CommentServiceImpl(
  private val staffService: StaffService,
  private val appService: AppService,
  private val commentRepository: CommentRepository,
  private val establishmentService: EstablishmentService,
) : CommentService {
  override fun addComment(
    prisonerId: String,
    staffId: String,
    appId: UUID,
    commentRequestDto: CommentRequestDto,
  ): CommentResponseDto<Any> {
    val staff = staffService.getStaffById(staffId).orElseThrow {
      ApiException("Staff with id $staffId does not exist", HttpStatus.NOT_FOUND)
    }
    val app = appService.getAppById(appId).orElseThrow {
      ApiException("App with id $appId does not exist", HttpStatus.FORBIDDEN)
    }
    validateStaffPermission(staff, app)
    validatePrisonerByRequestedBy(prisonerId, app)
    val comment = commentRepository.save(
      Comment(
        UUID.randomUUID(),
        commentRequestDto.message,
        LocalDateTime.now(ZoneOffset.UTC),
        staffId,
        appId,
      ),
    )
    app.comments.add(comment.id)
    appService.saveApp(app)
    return convertCommentToCommentResponseDto(prisonerId, staff.username, comment)
  }

  override fun getCommentById(
    prisonerId: String,
    staffId: String,
    appId: UUID,
    createdBy: Boolean,
    commentId: UUID,
  ): CommentResponseDto<Any> {
    val staff = getStaff(staffId)
    val app = appService.getAppById(appId).orElseThrow {
      ApiException("App with id $appId does not exist", HttpStatus.FORBIDDEN)
    }
    validateStaffPermission(staff, app)
    val comment = commentRepository.findById(commentId).orElseThrow {
      throw ApiException("Comment with id $commentId does not exist", HttpStatus.NOT_FOUND)
    }
    if (createdBy) {
      val staffWhoCreated = staffService.getStaffById(comment.createdBy).orElseThrow {
        ApiException("Staff who created with id ${comment.createdBy} does not exist", HttpStatus.NOT_FOUND)
      }
      val establishment = establishmentService.getEstablishmentById(staffWhoCreated.establishmentId).orElseThrow {
        ApiException("Establishment of Staff who created comment do not exist ${staffWhoCreated.establishmentId}", HttpStatus.NOT_FOUND)
      }
      val staffDto = StaffDto(
        staffWhoCreated.username,
        staffWhoCreated.userId,
        "${staff.fullName}",
        UserCategory.STAFF,
        establishment,
      )
      return convertCommentToCommentResponseDto(prisonerId, staffDto, comment)
    } else {
      return convertCommentToCommentResponseDto(prisonerId, comment.createdBy, comment)
    }
  }

  override fun getCommentsByAppId(
    prisonerId: String,
    staffId: String,
    appId: UUID,
    createdBy: Boolean,
    pageNumber: Long,
    pageSize: Long,
  ): PageResultComments {
    val staff = getStaff(staffId)
    val app = appService.getAppById(appId).orElseThrow {
      ApiException("App with id $appId does not exist", HttpStatus.FORBIDDEN)
    }
    validateStaffPermission(staff, app)
    validatePrisonerByRequestedBy(prisonerId, app)
    val pageRequest = PageRequest.of(pageNumber.toInt() - 1, pageSize.toInt())
    val pageResult = commentRepository.getCommentsByAppId(appId, pageRequest)
    return PageResultComments(
      (pageResult.pageable.pageNumber + 1),
      pageResult.totalElements.toLong(),
      pageResult.isLast,
      convertCommentsToCommentResponseDtoList(prisonerId, createdBy, pageResult.content),
    )
  }

  private fun convertCommentToCommentResponseDto(
    prisonerId: String,
    staff: Any,
    comment: Comment,
  ): CommentResponseDto<Any> = CommentResponseDto(
    comment.id,
    comment.appId,
    comment.message,
    prisonerId,
    comment.createdDate,
    staff,
  )

  private fun convertCommentsToCommentResponseDtoList(
    prisonerId: String,
    createdBy: Boolean,
    comments: List<Comment>,
  ): List<CommentResponseDto<Any>> {
    val list = ArrayList<CommentResponseDto<Any>>()
    comments.forEach { comment ->
      var createdByPerson: Any = comment.createdBy
      if (createdBy) {
        staffService.getStaffById(comment.createdBy).ifPresent { staff ->
          val establishment = establishmentService.getEstablishmentById(staff.establishmentId).orElseThrow {
            ApiException("Establishment not added for  id ${staff.establishmentId}", HttpStatus.BAD_REQUEST)
          }
          createdByPerson = StaffDto(
            staff.username,
            staff.userId,
            "${staff.fullName}",
            UserCategory.STAFF,
            establishment,
          )
        }
      }
      list.add(
        CommentResponseDto(
          comment.id,
          comment.appId,
          comment.message,
          prisonerId,
          comment.createdDate,
          createdByPerson,
        ),
      )
    }
    return list
  }

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
