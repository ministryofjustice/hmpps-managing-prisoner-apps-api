package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import java.io.Serializable
import jakarta.persistence.Embeddable

@Embeddable
data class EstablishmentApplicationGroupId(
  val establishmentId: String,
  val applicationGroupId: Long,
) : Serializable
