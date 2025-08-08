package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppByAppTypeCounts
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppByAssignedGroupCounts
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
      " AND (:firstNightCenter IS NULL OR a.firstNightCenter =:firstNightCenter)" +
      " GROUP BY a.appType ORDER BY a.appType ASC",
    nativeQuery = false,
  )
  fun countBySearchFilterGroupByAppType(
    establishmentId: String,
    status: Set<AppStatus>,
    appTypes: Set<AppType>?,
    requestedBy: String?,
    assignedGroups: Set<UUID>?,
    firstNightCenter: Boolean?,
  ): List<AppByAppTypeCounts>

  @Query(
    value = "SELECT COUNT(*) as count, a.assignedGroup as assignedGroup FROM App a " +
      " WHERE a.establishmentId = :establishmentId " +
      " AND a.status IN (:status)" +
      " AND (:appTypes IS NULL OR a.appType IN (:appTypes))" +
      " AND (:requestedBy IS NULL OR a.requestedBy = :requestedBy)" +
      " AND (:assignedGroups IS NULL OR a.assignedGroup IN (:assignedGroups))" +
      " AND (:firstNightCenter IS NULL OR a.firstNightCenter =:firstNightCenter)" +
      " GROUP BY a.assignedGroup",
    nativeQuery = false,
  )
  fun countBySearchFilterGroupByAssignedGroup(
    establishmentId: String,
    status: Set<AppStatus>,
    appTypes: Set<AppType>?,
    requestedBy: String?,
    assignedGroups: Set<UUID>?,
    firstNightCenter: Boolean?,
  ): List<AppByAssignedGroupCounts>

  @Query(
    value = "SELECT a FROM App a" +
      " WHERE a.establishmentId = :establishmentId" +
      " AND a.status IN (:status)" +
      " AND (:appTypes IS NULL OR a.appType IN (:appTypes))" +
      " AND (:requestedBy IS NULL OR a.requestedBy = :requestedBy)" +
      " AND (:assignedGroups IS NULL OR a.assignedGroup IN (:assignedGroups))" +
      " AND (:firstNightCenter IS NULL OR a.firstNightCenter =:firstNightCenter)" +
      " ORDER BY a.requestedDate DESC",
    nativeQuery = false,
  )
  fun appsBySearchFilter(
    establishmentId: String,
    status: Set<AppStatus>,
    appTypes: Set<AppType>?,
    requestedBy: String?,
    assignedGroups: Set<UUID>?,
    firstNightCenter: Boolean?,
    pageable: Pageable,
  ): Page<App>

  @Query(
    value = "select  DISTINCT a.requestedBy as requestedBy, a.requestedByFirstName as requestedByFirstName, a.requestedByLastName as requestedByLastName FROM App a" +
      " where a.establishmentId = :establishmentId" +
      " AND (a.requestedByFirstName ilike  %:searchText% OR a.requestedByLastName ilike  %:searchText%)" +
      " Order By a.requestedByFirstName, a.requestedByLastName",
    nativeQuery = false,
  )
  fun searchRequestedByFirstOrLastName(establishmentId: String, searchText: String): List<RequestedByNameSearchResult>

  fun findAppsByRequestedBy(requestedBy: String): List<App>
}
