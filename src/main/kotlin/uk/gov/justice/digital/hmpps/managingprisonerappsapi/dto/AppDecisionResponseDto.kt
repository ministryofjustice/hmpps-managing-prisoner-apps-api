package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Decision
import java.time.LocalDateTime
import java.util.*

data class AppDecisionResponseDto<T>(
  val id: UUID,
  val prisonerId: String,
  val appId: UUID,
  val reason: String,
  val decision: Decision,
  val createdDate: LocalDateTime,
  val createdBy: T,
  val appliesTo: List<UUID>
)
