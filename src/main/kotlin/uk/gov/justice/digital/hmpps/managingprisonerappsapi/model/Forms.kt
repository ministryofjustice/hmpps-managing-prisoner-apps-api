package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

data class Forms(
  val formMap: Map<String, Form>,
)

data class DataField(
  val id: Int,
  val name: String,
  val display: String,
  val type: String,
  val value: String,
  val values: List<String>?,
  val required: Boolean,
  val dependsOn: Int?,
)

data class Form(
  val dataFields: List<DataField>,
)
