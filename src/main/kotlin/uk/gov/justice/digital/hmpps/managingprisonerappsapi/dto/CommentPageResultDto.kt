package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto

data class CommentPageResultDto(
  val page: Int,
  val totalRecords: Long,
  val exhausted: Boolean,
  val comments: List<CommentResponseDto<Any>>
)
