package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class EstablishmentApplicationGroupId(
  val establishmentId: String,
  val applicationGroupId: Long,
) : Serializable
