package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import java.util.*

class CommentServiceImpl : CommentService {
  override fun addComment(prisonerId: String, staffId: String, appId: UUID, commentId: UUID, comment: Comment) {
    TODO("Not yet implemented")
  }

  override fun getCommentById(prisonerId: String, staffId: String, appId: UUID, commentId: UUID): Comment? {
    TODO("Not yet implemented")
  }

  override fun getCommentByAppId(staffId: String, appId: UUID): Comment? {
    TODO("Not yet implemented")
  }

}