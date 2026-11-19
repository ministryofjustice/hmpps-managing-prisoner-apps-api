package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.integration.dto.locations

import com.fasterxml.jackson.annotation.JsonProperty

data class LocationPrisonerDto(
  @JsonProperty("prisonerNumber")
  val prisonerNumber: String,

  @JsonProperty("prisonId")
  val prisonId: String? = null,

  @JsonProperty("prisonName")
  val prisonName: String? = null,

  @JsonProperty("cellLocation")
  val cellLocation: String? = null,

  @JsonProperty("firstName")
  val firstName: String? = null,

  @JsonProperty("lastName")
  val lastName: String? = null,

  @JsonProperty("gender")
  val gender: String? = null,

  @JsonProperty("status")
  val status: String? = null,

  @JsonProperty("inOutStatus")
  val inOutStatus: String? = null,

  @JsonProperty("csra")
  val csra: String? = null,

  @JsonProperty("category")
  val category: String? = null,

  @JsonProperty("alerts")
  val alerts: List<AlertDto>? = null,

  @JsonProperty("lastMovementTypeCode")
  val lastMovementTypeCode: String? = null,

  @JsonProperty("lastMovementDate")
  val lastMovementDate: String? = null,
)

data class AlertDto(
  @JsonProperty("alertType")
  val alertType: String? = null,

  @JsonProperty("alertCode")
  val alertCode: String? = null,

  @JsonProperty("active")
  val active: Boolean? = null,

  @JsonProperty("expired")
  val expired: Boolean? = null,
)
