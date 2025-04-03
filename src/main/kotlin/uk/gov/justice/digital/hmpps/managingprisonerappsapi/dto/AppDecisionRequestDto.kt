package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Decision
import java.util.*

data class AppDecisionRequestDto(
  val reason: String,
  val decision: Decision,
  val appliesTo: List<UUID>,
)
