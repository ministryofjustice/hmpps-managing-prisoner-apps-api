package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include

data class ApplicationTypeResponse(
  val id: Long,
  val name: String,
  @JsonInclude(Include.NON_NULL)
  val genericType: Boolean?,
  @JsonInclude(Include.NON_NULL)
  val logDetailRequired: Boolean?,
  @JsonInclude(Include.NON_NULL)
  val count: Long?,
)
