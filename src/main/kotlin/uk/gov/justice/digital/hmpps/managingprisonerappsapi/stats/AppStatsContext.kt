package uk.gov.justice.digital.hmpps.managingprisonerappsapi.stats

import java.util.UUID

data class AppStatsContext(
  val appId: UUID,
  val establishment: String,
  val appTypeId: Long,
  val appTypeName: String,
  val appGroupId: Long,
  val appGroupName: String,
  val department: String,
)
