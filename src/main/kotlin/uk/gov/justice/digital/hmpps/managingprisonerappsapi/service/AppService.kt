package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppResponseListDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.RequestedByNameSearchResult
import java.util.UUID

interface AppService {
  fun submitApp(prisonerId: String, staffId: String, appRequestDto: AppRequestDto): AppResponseDto

  fun getAppsById(prisonerId: String, id: UUID, requestedBy: Boolean, assignedGroup: Boolean): AppResponseDto

  fun getAppsByEstablishment(name: String): AppResponseDto

  fun forwardAppToGroup(groupId: UUID, appId: UUID): AppResponseDto

  fun searchAppsByColumnsFilter(
    staffId: String,
    status: Set<AppStatus>,
    appTypes: Set<AppType>?,
    requestedBy: String?,
    assignedGroups: Set<UUID>?,
    pageNumber: Long,
    pageSize: Long,
    ): AppResponseListDto

  fun searchRequestedByTextSearch(
    staffId: String,
    text: String,
  ): List<RequestedByNameSearchResult>
}
