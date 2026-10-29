package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response

import java.util.UUID

data class EstablishmentApplicationTypeResponseDto(
  val id: UUID,
  val establishmentId: String,
  val departmentSlug: String?,
  val active: Boolean,
  val applicationGroupResponse: ApplicationGroupResponse,
)
