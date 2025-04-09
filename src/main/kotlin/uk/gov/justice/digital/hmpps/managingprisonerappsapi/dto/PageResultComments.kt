package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto

data class PageResultComments(
  val page: Int,
  val totalElements: Long,
  val exhausted: Boolean,
  val contents: List<CommentResponseDto<Any>>,
)
