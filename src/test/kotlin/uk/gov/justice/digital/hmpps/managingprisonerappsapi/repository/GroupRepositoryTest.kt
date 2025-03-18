package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Groups
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator

@SpringBootTest(classes = [AppRepository::class])
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = ["uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository"])
@EntityScan("uk.gov.justice.digital.hmpps.managingprisonerappsapi.model")
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
class GroupRepositoryTest(@Autowired private val groupRepository: GroupRepository) {

  fun `save groups`() {
    val establishmentId = "HTS"
    val initialApps = listOf<AppType>(AppType.PIN_PHONE_ADD_NEW_CONTACT, AppType.PIN_PHONE_REMOVE_CONTACT)
    val groups = groupRepository.save(DataGenerator.generateGroups(establishmentId, initialApps, GroupType.WING))
    assertGroups(groups)
  }

  fun `update groups`() {
    val establishmentId = "HTS"
    val initialApps = listOf<AppType>(AppType.PIN_PHONE_ADD_NEW_CONTACT, AppType.PIN_PHONE_REMOVE_CONTACT)
    val groups = groupRepository.save(DataGenerator.generateGroups(establishmentId, initialApps, GroupType.WING))
    assertGroups(groups)
  }

  fun `delete groups`() {
    val establishmentId = "HTS"
    val initialApps = listOf<AppType>(AppType.PIN_PHONE_ADD_NEW_CONTACT, AppType.PIN_PHONE_REMOVE_CONTACT)
    val groups = groupRepository.save(DataGenerator.generateGroups(establishmentId, initialApps, GroupType.WING))
    assertGroups(groups)
    groupRepository.deleteById(groups.id)
    val findGroups = groupRepository.findById(groups.id)
    Assertions.assertTrue(findGroups.isEmpty())
  }

  fun `get group by id`() {
    val establishmentId = "HTS"
    val initialApps = listOf<AppType>(AppType.PIN_PHONE_ADD_NEW_CONTACT, AppType.PIN_PHONE_REMOVE_CONTACT)
    val groups = groupRepository.save(DataGenerator.generateGroups(establishmentId, initialApps, GroupType.WING))
    assertGroups(groups)
    val findGroups = groupRepository.findById(groups.id)
    Assertions.assertTrue(findGroups.isPresent)
  }

  fun `get groups by initial apps`() {

  }

  fun assertGroups(groups: Groups) {
    Assertions.assertNotNull(groups.id)
    Assertions.assertNotNull(groups.name)
    Assertions.assertNotNull(groups.establishmentId)
    Assertions.assertNotNull(groups.type)
    Assertions.assertNotNull(groups.initialsApps)
  }

}