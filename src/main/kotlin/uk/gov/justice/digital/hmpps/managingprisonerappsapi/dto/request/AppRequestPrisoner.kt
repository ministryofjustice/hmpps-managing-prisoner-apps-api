package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

data class AppRequestPrisoner(
  val reference: String? = null,
  val applicationType: Long?,
  val genericForm: Boolean,
  val requests: List<MutableMap<String, Any>>,
)
