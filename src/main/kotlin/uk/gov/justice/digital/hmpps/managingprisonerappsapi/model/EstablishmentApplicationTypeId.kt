package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class EstablishmentApplicationTypeId(
  val establishmentId: String,
  @Column(name = "application_group_id", insertable = false, updatable = false)
  val applicationGroupId: Long,
  val applicationTypeId: Long,
) : Serializable
