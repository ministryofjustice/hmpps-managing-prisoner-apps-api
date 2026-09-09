package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App

data class PrisonerAppRow(
  val app: App,
  val appType: String,
  val commentCount: Long,
)
