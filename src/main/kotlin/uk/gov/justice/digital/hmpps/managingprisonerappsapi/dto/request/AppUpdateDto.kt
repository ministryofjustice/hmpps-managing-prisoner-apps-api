package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request

data class AppUpdateDto(
  val firstNightCenter: Boolean,
  val formDat:List<Map<String, Any>>
)
