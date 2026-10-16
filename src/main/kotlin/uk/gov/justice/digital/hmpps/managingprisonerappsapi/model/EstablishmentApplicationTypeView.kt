package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

interface EstablishmentApplicationTypeView {
  val applicationGroupId: Long
  val applicationTypeId: Long
  val active: Boolean
}
