package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

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
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment

const val ESTABLISHMENT_ID_1 = "TS1"
const val ESTABLISHMENT_ID_2 = "TS2"
const val ESTABLISHMENT_NAME_1 = "TEST_ESTABLISHMENT_1"
const val ESTABLISHMENT_NAME_2 = "TEST_ESTABLISHMENT_2"

@SpringBootTest(classes = [GroupRepository::class, EstablishmentRepository::class])
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = ["uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository"])
@EntityScan("uk.gov.justice.digital.hmpps.managingprisonerappsapi.model")
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
class EstablishmentRepositoryTest(@Autowired private val establishmentRepository: EstablishmentRepository) {

  @BeforeEach
  fun setUp() {
    establishmentRepository.deleteAll()
  }

  @Test
  fun `save establishment`() {
    val establishment = Establishment(
      ESTABLISHMENT_ID_1,
      ESTABLISHMENT_NAME_1,
      setOf(AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT),
      false,
      setOf(),
      setOf(),
    )
    val entity = establishmentRepository.save(establishment)
    assertEstablishment(establishment, entity)
  }

  @Test
  fun `update establishment`() {
    val establishment = Establishment(ESTABLISHMENT_ID_1, ESTABLISHMENT_NAME_1, AppType.entries.toSet(), false, setOf(), setOf())
    val entity = establishmentRepository.save(establishment)
    val updatedEstablishment = Establishment(entity.id, ESTABLISHMENT_NAME_2, AppType.entries.toSet(), false, setOf(), setOf())
    val updatedEntity = establishmentRepository.save(updatedEstablishment)
    assertEstablishment(updatedEstablishment, updatedEntity)
  }

  @Test
  fun `get establishment by id`() {
    val establishment = Establishment(ESTABLISHMENT_ID_1, ESTABLISHMENT_NAME_1, AppType.entries.toSet(), false, setOf(), setOf())
    establishmentRepository.save(establishment)
    val entity = establishmentRepository.findById(establishment.id)
    assertEstablishment(establishment, entity.get())
  }

  @Test
  fun `delete establishment by id`() {
    val establishment = Establishment(ESTABLISHMENT_ID_1, ESTABLISHMENT_NAME_1, AppType.entries.toSet(), false, setOf(), setOf())
    establishmentRepository.save(establishment)
    establishmentRepository.deleteById(establishment.id)
    val entity = establishmentRepository.findById(establishment.id)
    Assertions.assertEquals(true, entity.isEmpty)
  }

  private fun assertEstablishment(expected: Establishment, actual: Establishment) {
    Assertions.assertEquals(expected.id, actual.id)
    Assertions.assertEquals(expected.name, actual.name)
    Assertions.assertEquals(expected.appTypes.size, actual.appTypes.size)
  }
}
