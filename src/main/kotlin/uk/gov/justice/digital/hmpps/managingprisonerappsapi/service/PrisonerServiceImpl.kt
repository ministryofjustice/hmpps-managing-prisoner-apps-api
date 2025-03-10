package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.RequestedByDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import java.util.*

@Service
class PrisonerServiceImpl : PrisonerService {
  override fun getPrisonerById(id: String): Optional<Prisoner> {
    // TODO("Not yet implemented")
    // Implement it later just adding placeholder for returning a prisoner
    val prisoner = Prisoner(
      id,
      UUID.randomUUID().toString(),
      "Test",
      "Prisoner",
      UserCategory.PRISONER,
      "test establishment",
      UUID.randomUUID().toString(),
    )
    return Optional.of(prisoner)
  }

  fun convertPrisonerToRequestByDto(prisoner: Prisoner): RequestedByDto = RequestedByDto(
    prisoner.username,
    prisoner.firstName,
    prisoner.lastName,
    prisoner.location,
    prisoner.iep,
  )
}
