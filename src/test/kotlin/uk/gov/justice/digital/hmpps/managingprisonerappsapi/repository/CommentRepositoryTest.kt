package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator
import java.util.*

@SpringBootTest(classes = [CommentRepository::class])
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = ["uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository"])
@EntityScan("uk.gov.justice.digital.hmpps.managingprisonerappsapi.model")
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
class CommentRepositoryTest(
  @Autowired var commentRepository: CommentRepository,
) {

  @BeforeEach
  @AfterEach
  fun setUp() {
    commentRepository.deleteAll()
  }

  @Test
  fun saveComment() {
    val id = UUID.randomUUID()
    val comment = DataGenerator.generateComment(id)
    val createdComment = commentRepository.save(comment)
    Assertions.assertNotNull(createdComment)
  }

  @Test
  fun getCommentBydId() {
    val createdComment = commentRepository.save(DataGenerator.generateComment(UUID.randomUUID()))
    val comment = commentRepository.findById(createdComment.id)
    Assertions.assertEquals(true, comment.isPresent)
  }

  @Test
  fun `update comment`() {
    var createdComment = commentRepository.save(DataGenerator.generateComment(UUID.randomUUID()))
    var comment = Comment(
      createdComment.id,
      "updating message",
      createdComment.createdDate,
      createdComment.createdBy,
      createdComment.users,
      createdComment.app,
    )
    comment = commentRepository.save(comment)
    Assertions.assertEquals("updating message", comment.message)
  }

  @Test
  fun deleteCommentById() {
    val createdComment = commentRepository.save(DataGenerator.generateComment(UUID.randomUUID()))
    commentRepository.deleteById(createdComment.id)
    val comment = commentRepository.findById(createdComment.id)
    Assertions.assertEquals(false, comment.isPresent)
  }
}
