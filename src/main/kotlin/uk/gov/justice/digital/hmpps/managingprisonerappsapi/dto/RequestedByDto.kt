package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto

data class RequestedByDto(
  val id: String,
  val firstName: String,
  val lastName: String,
  val location: String,
  val iep: String,
)
