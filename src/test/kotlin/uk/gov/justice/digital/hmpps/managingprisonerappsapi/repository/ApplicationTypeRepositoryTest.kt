package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

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
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationGroup
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationType

@SpringBootTest(classes = [ApplicationGroup::class, ApplicationType::class])
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = ["uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository"])
@EntityScan("uk.gov.justice.digital.hmpps.managingprisonerappsapi.model")
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
class ApplicationTypeRepositoryTest(
  @Autowired val applicationGroupRepository: ApplicationGroupRepository,
  @Autowired val applicationTypeRepository: ApplicationTypeRepository,
) {
  @Test
  fun `save and update application type`() {
    val applicationType = ApplicationType(1, "Add new social PIN phone contact", false, false)
    applicationTypeRepository.save<ApplicationType>(applicationType)
    val applicationGroup = ApplicationGroup(1, "MDI", listOf(ApplicationType(1, "Add new social PIN phone contact", false, false)))
    applicationGroupRepository.save<ApplicationGroup>(applicationGroup)
    applicationType.applicationGroup = applicationGroup
    applicationGroup.applicationTypes = listOf(applicationType)
    applicationTypeRepository.save(applicationType)
    Assertions.assertNotNull(applicationTypeRepository.findById(1).get())
  }
}
