package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.History
import java.util.UUID

@Repository
interface HistoryRepository : JpaRepository<History, UUID> {

  fun findByAppIdAndEstablishmentOrderByCreatedDate(appId: UUID, establishment: String): List<History>
}
