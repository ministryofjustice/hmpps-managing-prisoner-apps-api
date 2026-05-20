package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory

data class PrisonerDto(
  val username: String,
  val userId: String?,
  val fullName: String,
  val category: UserCategory,
  val establishmentId: String,
)
