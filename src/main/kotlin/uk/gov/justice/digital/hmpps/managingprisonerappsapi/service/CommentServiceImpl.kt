package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.CommentRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.CommentResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.PageResultComments
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.collections.ArrayList

@Service
class CommentServiceImpl(
  private val prisonerService: PrisonerService,
  private val staffService: StaffService,
  private val commentRepository: CommentRepository,
) : CommentService {
  override fun addComment(prisonerId: String, staffId: String, appId: UUID, commentRequestDto: CommentRequestDto): CommentResponseDto<Any> {

    val prisoner = prisonerService.getPrisonerById(prisonerId).orElseThrow {
      ApiException("Prison with id $prisonerId does not exist", HttpStatus.FORBIDDEN)
    }
    val staff = staffService.getStaffById(staffId).orElseThrow {
      ApiException("Staff with id $staffId does not exist", HttpStatus.NOT_FOUND)
    }
    if (prisoner.establishmentId != staff.establishmentId) {
      throw ApiException("Staff and prisoner is from two different establishment", HttpStatus.FORBIDDEN)
    }
    val comment = commentRepository.save(Comment(UUID.randomUUID(), commentRequestDto.message, LocalDateTime.now(ZoneOffset.UTC), staffId, appId))
    return convertCommentToCommentResponseDto(prisonerId, staff.userId, comment)
    }

  override fun getCommentById(prisonerId: String, staffId: String, appId: UUID, createdBy: Boolean, commentId: UUID): CommentResponseDto<Any> {
    val prisoner = prisonerService.getPrisonerById(prisonerId).orElseThrow {
      ApiException("Prison with id $prisonerId does not exist", HttpStatus.FORBIDDEN)
    }
    val staff = staffService.getStaffById(staffId).orElseThrow {
      ApiException("Staff with id $staffId does not exist", HttpStatus.NOT_FOUND)
    }
    if (prisoner.establishmentId != staff.establishmentId) {
      throw ApiException("Staff and prisoner is from two different establishment", HttpStatus.FORBIDDEN)
    }
    val comment = commentRepository.findById(commentId).orElseThrow {
      throw ApiException("Comment with id $commentId does not exist", HttpStatus.NOT_FOUND)
    }
    if (createdBy) {
      val staffWhoCreated = staffService.getStaffById(comment.createdBy).orElseThrow {
        ApiException("Staff who created with id ${comment.createdBy} does not exist", HttpStatus.NOT_FOUND)
      }
      return convertCommentToCommentResponseDto(prisonerId, staffWhoCreated, comment)
    } else {
      return convertCommentToCommentResponseDto(prisonerId, comment.createdBy, comment)
    }
  }

  override fun getCommentsByAppId(prisonerId: String, staffId: String, appId: UUID, createdBy: Boolean, pageNumber: Long, pageSize: Long): PageResultComments {
    val prisoner = prisonerService.getPrisonerById(prisonerId).orElseThrow {
      ApiException("Prison with id $prisonerId does not exist", HttpStatus.FORBIDDEN)
    }
    val staff = staffService.getStaffById(staffId).orElseThrow {
      ApiException("Staff with id $staffId does not exist", HttpStatus.NOT_FOUND)
    }
    if (prisoner.establishmentId != staff.establishmentId) {
      throw ApiException("Staff and prisoner is from two different establishment", HttpStatus.FORBIDDEN)
    }
    val pageRequest = PageRequest.of(pageNumber.toInt() - 1, pageSize.toInt())
    val pageResult = commentRepository.getCommentsByAppId(appId, pageRequest)
    return PageResultComments(
      (pageResult.pageable.pageNumber + 1),
      pageResult.totalElements.toLong(),
      pageResult.isLast,
      convertCommentsToCommentResponseDtoList(prisonerId, createdBy, pageResult.content)
    )
  }

  private fun convertCommentToCommentResponseDto(prisonerId: String, staff: Any, comment: Comment): CommentResponseDto<Any> {
    return CommentResponseDto(
      comment.id,
      comment.appId,
      comment.message,
      prisonerId,
      comment.createdDate,
      staff,
    )
  }

  private fun convertCommentsToCommentResponseDtoList(prisonerId: String, createdBy: Boolean, comments: List<Comment>): List<CommentResponseDto<Any>> {
    val list = ArrayList<CommentResponseDto<Any>>()
    comments.forEach {  comment ->
      var createdByPerson: Any = comment.createdBy
      if (createdBy) {
        staffService.getStaffById(comment.createdBy).ifPresent {  staff -> createdByPerson = staff}
      }
      list.add(
        CommentResponseDto(
          comment.id,
          comment.appId,
          comment.message,
          prisonerId,
          comment.createdDate,
          createdByPerson,
        )
      )
    }
    return list
  }
}