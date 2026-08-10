package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus

data class AppStatusUpdateDto(
  val status: AppStatus,
  val comment: String? = null,
)
