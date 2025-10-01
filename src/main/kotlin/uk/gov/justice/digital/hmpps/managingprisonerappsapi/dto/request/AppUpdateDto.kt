package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request

data class AppUpdateDto(
  val firstNightCenter: Boolean,
  val formData: List<Map<String, Any>>,
)
