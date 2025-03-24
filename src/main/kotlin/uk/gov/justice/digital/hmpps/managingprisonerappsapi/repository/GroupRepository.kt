package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Groups
import java.util.UUID

@Repository
interface GroupRepository : JpaRepository<Groups, UUID> {

  fun findGroupsByEstablishmentIdAndInitialsAppsIsContaining(establishmentId: String, initialsApp: AppType): List<Groups>

  fun getGroupsByEstablishmentId(id: String): List<Groups>
}
