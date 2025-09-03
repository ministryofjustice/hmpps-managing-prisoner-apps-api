package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.*

data class AppRequestDto(
  val reference: String? = null,
  val type: String,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val requestedDate: LocalDateTime?,
  val requests: List<MutableMap<String, Any>>,
  @Schema(
    required = false,
    description = "Pass this value only for type PIN_PHONE_ADD_NEW_SOCIAL_CONTACT. No value passed is set as false. For other type this value is always false.",
  )
  val firstNightCenter: Boolean?,
  val department: UUID?,
)
