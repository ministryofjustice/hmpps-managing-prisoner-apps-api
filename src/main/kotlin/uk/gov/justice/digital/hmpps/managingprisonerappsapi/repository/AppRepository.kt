package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppByAppTypeCounts
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.RequestedByNameSearchResult
import java.util.*

@Repository
interface AppRepository : JpaRepository<App, UUID> {

  fun findAppsByIdAndRequestedBy(id: UUID, requestedBy: String): Optional<App>

  @Query(
    value = "SELECT COUNT(*) as count, a.appType as appType FROM App a " +
      " WHERE a.establishmentId = :establishmentId " +
      " AND a.status IN (:status)" +
      " AND (:appTypes IS NULL OR a.appType IN (:appTypes))" +
      " AND (:requestedBy IS NULL OR a.requestedBy = :requestedBy)" +
      " AND (:assignedGroups IS NULL OR a.assignedGroup IN (:assignedGroups))" +
      " GROUP BY a.appType ORDER BY a.appType ASC",
    nativeQuery = false,
  )
  fun countBySearchFilter(
    establishmentId: String,
    status: Set<AppStatus>,
    appTypes: Set<AppType>?,
    requestedBy: String?,
    assignedGroups: Set<UUID>?,
  ): List<AppByAppTypeCounts>

  @Query(
    value = "SELECT a FROM App a" +
      " WHERE a.establishmentId = :establishmentId" +
      " AND a.status IN (:status)" +
      " AND (:appTypes IS NULL OR a.appType IN (:appTypes))" +
      " AND (:requestedBy IS NULL OR a.requestedBy = :requestedBy)" +
      " AND (:assignedGroups IS NULL OR a.assignedGroup IN (:assignedGroups))" +
      " ORDER BY a.requestedDate ASC",
    nativeQuery = false,
  )
  fun appsBySearchFilter(
    establishmentId: String,
    status: Set<AppStatus>,
    appTypes: Set<AppType>?,
    requestedBy: String?,
    assignedGroups: Set<UUID>?,
    pageable: Pageable,
  ): Page<App>

  @Query(
    value = "select  DISTINCT a.requestedBy as requestedBy, a.requestedByFullName as requestedByFullName FROM App a" +
      " where a.establishmentId = :establishmentId" +
      " AND a.requestedByFullName ilike  %:searchText%" +
      " Order By a.requestedByFullName",
    nativeQuery = false,
  )
  fun searchRequestedByFullName(establishmentId: String, searchText: String): List<RequestedByNameSearchResult>

}
