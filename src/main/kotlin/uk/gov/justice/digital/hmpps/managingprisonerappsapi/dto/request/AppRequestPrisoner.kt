package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request

data class AppRequestPrisoner(
  val reference: String? = null,
  val applicationType: Long?,
  val genericForm: Boolean,
  val requests: List<MutableMap<String, Any>>,
)
