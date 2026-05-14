package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.CommentVisibility
import java.util.*

@Repository
interface CommentRepository : JpaRepository<Comment, UUID> {
  fun getCommentsByAppId(appId: UUID, pageable: Pageable): Page<Comment>

  fun getCommentByIdAndVisibility(id: UUID, visibility: CommentVisibility): Optional<Comment>

  fun getCommentsByAppIdAndVisibility(appId: UUID, visibility: CommentVisibility, pageable: Pageable): Page<Comment>
}
