package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.*

@Entity
data class EstablishmentRole(
  @Id
  val id: UUID,
  val establishment: UUID,
  val role: Role
)
