package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.CommentRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.CommentResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.PageResultComments
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import java.util.*

interface CommentService {
  fun addComment(prisonerId: String, staffId: String, appId: UUID, commentRequestDto: CommentRequestDto): CommentResponseDto<Any>

  fun getCommentById(prisonerId: String, staffId: String, appId: UUID, createdBy: Boolean, commentId: UUID): CommentResponseDto<Any>

  fun getCommentsByAppId(prisonerId: String, staffId: String, appId: UUID, createdBy: Boolean, pageNumber: Long, pageSize: Long): PageResultComments
}