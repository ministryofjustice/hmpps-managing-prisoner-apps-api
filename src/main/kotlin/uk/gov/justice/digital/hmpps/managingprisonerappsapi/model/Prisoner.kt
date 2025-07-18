package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

data class Prisoner(
  val username: String,
  val userId: String,
  val firstName: String,
  val lastName: String,
  val category: UserCategory,
  val cellLocation: String?,
  val location: String?,
  val iep: String?,
  val establishmentId: String?,
  val alerts: Int,
)
