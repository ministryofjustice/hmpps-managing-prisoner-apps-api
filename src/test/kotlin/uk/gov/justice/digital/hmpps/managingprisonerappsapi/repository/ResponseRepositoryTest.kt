package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import com.fasterxml.uuid.Generators
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Response
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator
import java.time.LocalDateTime
import java.util.*

@SpringBootTest(classes = [ResponseRepositoryTest::class])
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = ["uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository"])
@EntityScan("uk.gov.justice.digital.hmpps.managingprisonerappsapi.model")
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
class ResponseRepositoryTest(@Autowired var responseRepository: ResponseRepository) {
  @Test
  fun `save response`() {
    val response = responseRepository.save(DataGenerator.generateResponse(Generators.timeBasedEpochGenerator().generate().toString()))
    Assertions.assertNotNull(response)
  }

  @Test
  fun `update response`() {
    val createdResponse = responseRepository.save(DataGenerator.generateResponse(Generators.timeBasedEpochGenerator().generate().toString()))
    var response = Response(
      createdResponse.id,
      "updating reason",
      createdResponse.decision,
      LocalDateTime.now(),
      createdResponse.createdBy,
    )
    response = responseRepository.save(response)
    Assertions.assertEquals("updating reason", response.reason)
  }

  @Test
  fun `find response by id`() {
    val createdResponse = responseRepository.save(DataGenerator.generateResponse(Generators.timeBasedEpochGenerator().generate().toString()))
    val response = responseRepository.findById(createdResponse.id)
    Assertions.assertEquals(true, response.isPresent)
  }

  @Test
  fun `delete response by id`() {
    val createdResponse = responseRepository.save(DataGenerator.generateResponse(Generators.timeBasedEpochGenerator().generate().toString()))
    responseRepository.deleteById(createdResponse.id)
    val response = responseRepository.findById(createdResponse.id)
    Assertions.assertEquals(false, response.isPresent)
  }
}
