package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import java.time.LocalDateTime

// This is response model for giving counts of given application type which are in pending and submitted by prisoner.
data class PrisonerApplicationTypeCount(
  val id: Long,
  val name: String,
  @JsonInclude(Include.NON_NULL)
  val genericType: Boolean?,
  @JsonInclude(Include.NON_NULL)
  val genericForm: Boolean?,
  @JsonInclude(Include.NON_NULL)
  val logDetailRequired: Boolean?,
  @JsonInclude(Include.NON_NULL)
  val totalAppsInPending: Long?,
  @JsonInclude(Include.NON_NULL)
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val latestAppSubmittedDate: LocalDateTime?,
  val submittedBy: UserCategory,
)
