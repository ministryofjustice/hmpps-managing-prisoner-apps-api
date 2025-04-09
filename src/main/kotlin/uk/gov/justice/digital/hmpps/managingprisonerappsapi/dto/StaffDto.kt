package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import java.util.*

data class StaffDto(
  val username: String,
  val userId: String,
  val fullName: String,
  val category: UserCategory,
  val establishment: EstablishmentDto,
)
