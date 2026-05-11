package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.integration.client

import com.fasterxml.jackson.annotation.JsonProperty

data class HmppsAuthAccessToken(
  @JsonProperty("access_token")
  val accessToken: String,
)
