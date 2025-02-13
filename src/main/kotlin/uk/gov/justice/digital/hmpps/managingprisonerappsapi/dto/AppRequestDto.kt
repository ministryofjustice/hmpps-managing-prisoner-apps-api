package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import java.time.LocalDateTime
import java.util.*

data class AppRequestDto(
  val reference: String,
  val type: String,
  val requestedDate: LocalDateTime,
  val requests: List<Map<String, Any>>,
  val createdBy: String
)
