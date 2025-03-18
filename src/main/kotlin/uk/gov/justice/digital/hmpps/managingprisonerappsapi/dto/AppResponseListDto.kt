package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import java.time.LocalDateTime
import java.util.UUID

data class AppResponseListDto(
 val appTypeList: List<AppTypeDto>,
  val apps: List<AppSearchDto>,
)

data class AppTypeDto(
  val number: Int,
  val appType: AppType,
)

data class AppSearchDto(
  val id: UUID,
  val requestedDate: LocalDateTime,
  val group: AssignedGroupDto,
  val status: AppStatus,
)


