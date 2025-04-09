package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import java.util.*

@Repository
interface CommentRepository : JpaRepository<Comment, UUID> {
  fun getCommentsByAppId(appId: UUID, pageable: Pageable): Page<Comment>
}
