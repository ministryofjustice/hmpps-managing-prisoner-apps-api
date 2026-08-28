package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.CommentResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PageResultComments
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import java.util.*

interface CommentService {

  fun saveComment(comment: Comment): Comment

  fun addCommentByStaff(prisonerId: String, staffId: String, appId: UUID, message: String): CommentResponseDto<Any>

  fun addMessageByStaff(prisonerId: String, staffId: String, appId: UUID, message: String): CommentResponseDto<Any>

  fun addMessageByPrisoner(prisonerId: String, appId: UUID, message: String): CommentResponseDto<Any>

  fun getCommentByIdForStaff(prisonerId: String, staffId: String, appId: UUID, createdBy: Boolean, commentId: UUID): CommentResponseDto<Any>

  fun getCommentByIdForPrisoner(prisonerId: String, appId: UUID, createdBy: Boolean, commentId: UUID): CommentResponseDto<Any>

  fun getCommentsByAppIdForStaff(prisonerId: String, staffId: String, appId: UUID, pageNumber: Long, pageSize: Long): PageResultComments

  fun getMessagesByAppIdForStaff(prisonerId: String, staffId: String, appId: UUID, pageNumber: Long, pageSize: Long): PageResultComments

  fun getMessagesByAppIdForPrisoner(prisonerId: String, appId: UUID, pageNumber: Long, pageSize: Long): PageResultComments
}
