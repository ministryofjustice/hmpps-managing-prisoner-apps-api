package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request

import java.util.UUID

data class EstablishmentApplicationTypeRequestDto(
  val id: UUID,
  val applicationTypeId: Long,
  val establishmentId: String,
  val departmentId: UUID?,
  val active: Boolean,
)
