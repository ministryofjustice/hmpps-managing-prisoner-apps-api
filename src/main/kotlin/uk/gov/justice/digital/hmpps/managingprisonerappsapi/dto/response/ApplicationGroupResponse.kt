package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response

data class ApplicationGroupResponse(
  val id: Long,
  val name: String,
  val appTypes: List<ApplicationTypeResponse>,
)
