package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.integration.dto.locations

import com.fasterxml.jackson.annotation.JsonProperty

data class LocationCellDto(
  @JsonProperty("cellLocation")
  val cellLocation: String,

  @JsonProperty("prisoners")
  val prisoners: List<LocationPrisonerDto> = emptyList(),
)
