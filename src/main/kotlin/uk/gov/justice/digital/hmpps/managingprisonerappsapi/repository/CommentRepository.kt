package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import java.util.UUID

interface CommentRepository : JpaRepository<Comment, UUID>{
}