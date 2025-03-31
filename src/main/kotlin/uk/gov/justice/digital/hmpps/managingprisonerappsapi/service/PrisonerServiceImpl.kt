package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.integration.client.PrisonerSearchClient
import java.util.Optional

@Service
class PrisonerServiceImpl(
  var prisonerSearchClient: PrisonerSearchClient,
) : PrisonerService {
  override fun getPrisonerById(prisonerId: String): Optional<Prisoner> {
    val prisonerDto = prisonerSearchClient.getPrisonerById(prisonerId)
    val prisoner = Prisoner(
      prisonerId,
      prisonerId,
      prisonerDto.firstName,
      prisonerDto.lastName,
      UserCategory.PRISONER,
      prisonerDto.locationDescription,
      prisonerDto.currentIncentive?.level?.description,
    )
    return Optional.of(prisoner)
  }
}
