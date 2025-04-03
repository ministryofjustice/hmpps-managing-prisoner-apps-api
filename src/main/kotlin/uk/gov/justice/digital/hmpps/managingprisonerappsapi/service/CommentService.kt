package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import java.util.*

interface CommentService {
  fun addComment(prisonerId: String, staffId: String, appId: UUID, commentId: UUID,comment: Comment)

  fun getCommentById(prisonerId: String, staffId: String, appId: UUID, commentId: UUID): Comment?

  fun getCommentByAppId(staffId: String, appId: UUID): Comment?
}