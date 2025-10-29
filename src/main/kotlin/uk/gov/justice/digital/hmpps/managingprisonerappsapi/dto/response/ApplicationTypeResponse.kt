package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response

data class ApplicationTypeResponse(
  val id: Long,
  val name: String,
  val genericType: Boolean,
  val logDetailRequired: Boolean,
)
