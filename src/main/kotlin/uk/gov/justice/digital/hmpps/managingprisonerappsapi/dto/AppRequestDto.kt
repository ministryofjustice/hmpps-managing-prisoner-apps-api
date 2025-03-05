package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto

import java.time.LocalDateTime

data class AppRequestDto(
  val reference: String,
  val type: String,
  val requestedDate: LocalDateTime,
  val requests: List<Map<String, Any>>,
)
