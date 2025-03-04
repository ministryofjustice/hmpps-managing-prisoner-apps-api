package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import java.util.*

interface PrisonerService {
  fun getPrisonerById(id: String): Optional<Prisoner>
}
