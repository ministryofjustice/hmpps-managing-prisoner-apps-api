package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request

import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.CommentVisibility

data class CommentRequestDto(
  @Size(max = 1000)
  val message: String,
  val visibility: CommentVisibility,
)
