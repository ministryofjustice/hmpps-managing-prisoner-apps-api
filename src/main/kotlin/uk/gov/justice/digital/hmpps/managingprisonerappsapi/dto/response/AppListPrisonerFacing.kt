package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import java.time.LocalDateTime
import java.util.UUID

data class PrisonerAppsPage(
  val page: Int,
  val totalRecords: Long,
  val exhausted: Boolean,
  val apps: List<AppListPrisonerFacing>,
)

data class AppListPrisonerFacing(
  val id: UUID,
  val prisonerId: String,
  val applicationType: String,
  val createdDate: LocalDateTime,
  val lastUpdatedDate: LocalDateTime,
  val status: AppStatus,
)
