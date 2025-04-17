package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request

import jakarta.validation.constraints.Size

data class CommentRequestDto(
  @Size(max = 1000)
  val message: String,
)
