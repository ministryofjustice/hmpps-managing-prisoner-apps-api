package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response

import java.util.UUID

data class EstablishmentApplicationTypeResponse(
  val id: UUID?,
  val establishmentId: String,
  val active: Boolean,
  val applicationGroupResponse: ApplicationGroupResponse,
)
