package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include

data class ApplicationGroupResponse(
  val id: Long,
  val name: String,
  @JsonInclude(Include.NON_NULL)
  val appTypes: List<ApplicationTypeResponse>?,
)
