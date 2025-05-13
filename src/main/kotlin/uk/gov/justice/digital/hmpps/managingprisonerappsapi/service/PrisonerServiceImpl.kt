package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.integration.client.PrisonerSearchClient
import java.util.*

@Service
class PrisonerServiceImpl(
  var prisonerSearchClient: PrisonerSearchClient,
) : PrisonerService {

  @Value("\${spring.profiles.active:test}")
  private lateinit var activeProfile: String

  override fun getPrisonerById(prisonerId: String): Optional<Prisoner> {
    val prisonerDto = prisonerSearchClient.getPrisonerById(prisonerId)
    val prisoner = Prisoner(
      prisonerId,
      prisonerDto.prisonerNumber,
      prisonerDto.firstName,
      prisonerDto.lastName,
      UserCategory.PRISONER,
      prisonerDto.locationDescription,
      prisonerDto.currentIncentive?.level?.description,
      if (activeProfile.equals("test") || activeProfile.equals("dev")) "TEST_ESTABLISHMENT_FIRST" else prisonerDto.prisonId,
      prisonerDto.alerts.size,
    )
    return Optional.of(prisoner)
  }
}
