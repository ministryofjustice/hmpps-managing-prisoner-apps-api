package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.CommentRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppResponseListDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.HistoryResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.RequestedByNameSearchResult
import java.util.*

interface AppService {
  fun submitApp(prisonerId: String, staffId: String, appRequestDto: AppRequestDto): AppResponseDto<Any, Any>

  fun saveApp(app: App): App

  fun updateAppFormData(prisonerId: String, staffId: String, appId: UUID, requestFormData: List<Map<String, Any>>): AppResponseDto<Any, Any>

  fun getAppById(appId: UUID): Optional<App>

  fun getAppsById(prisonerId: String, appId: UUID, staffId: String, requestedBy: Boolean, assignedGroup: Boolean): AppResponseDto<Any, Any>

  fun getHistoryAppsId(prisonerId: String, appId: UUID, staffId: String): List<HistoryResponse>

  fun forwardAppToGroup(staffId: String, groupId: UUID, appId: UUID, commentRequestDto: CommentRequestDto?): AppResponseDto<Any, Any>

  fun searchAppsByColumnsFilter(
    staffId: String,
    status: Set<AppStatus>,
    appTypes: Set<AppType>?,
    requestedBy: String?,
    assignedGroups: Set<UUID>?,
    firstNightCenter: Boolean?,
    pageNumber: Long,
    pageSize: Long,
  ): AppResponseListDto

  fun searchRequestedByTextSearch(
    staffId: String,
    text: String,
  ): List<RequestedByNameSearchResult>
}
