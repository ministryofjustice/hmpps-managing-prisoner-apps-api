package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment

@Repository
interface EstablishmentRepository: JpaRepository<Establishment, String> {
}