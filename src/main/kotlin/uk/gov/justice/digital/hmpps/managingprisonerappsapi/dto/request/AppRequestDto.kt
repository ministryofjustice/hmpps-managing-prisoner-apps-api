package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class AppRequestDto(
  val reference: String? = null,
  val type: String,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val requestedDate: LocalDateTime,
  val requests: List<MutableMap<String, Any>>,
)
