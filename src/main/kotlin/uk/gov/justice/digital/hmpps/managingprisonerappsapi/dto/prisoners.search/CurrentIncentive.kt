package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search

data class Alias(
  val title: String? = null,
  val firstName: String? = null,
  val middleNames: String? = null,
  val lastName: String? = null,
  val dateOfBirth: String? = null,
  val gender: String? = null,
  val ethnicity: String? = null,
  val raceCode: String? = null,
)

data class Alert(
  val alertType: String? = null,
  val alertCode: String? = null,
  val active: Boolean? = false,
  val expired: Boolean? = false,
)

data class CurrentIncentive(
  val level: Level,
  val dateTime: String? = null,
  val nextReviewDate: String? = null,
)

data class Level(
  val code: String? = null,
  val description: String? = null,
)

data class Tattoo(
  val bodyPart: String? = null,
  val comment: String? = null,
)

data class Scar(
  val bodyPart: String? = null,
  val comment: String? = null,
)

data class Mark(
  val bodyPart: String? = null,
  val comment: String? = null,
)

data class Address(
  val fullAddress: String? = null,
  val postalCode: String? = null,
  val startDate: String? = null,
  val primaryAddress: Boolean? = false,
  val noFixedAddress: Boolean? = false,
  val phoneNumbers: List<PhoneNumber>,
)

data class PhoneNumber(
  val type: String? = null,
  val number: String? = null,
)

data class EmailAddress(
  val email: String? = null,
)

data class PhoneNumber2(
  val type: String? = null,
  val number: String? = null,
)

data class Identifier(
  val type: String? = null,
  val value: String? = null,
  val issuedDate: String? = null,
  val issuedAuthorityText: String? = null,
  val createdDateTime: String? = null,
)

data class AllConvictedOffence(
  val statuteCode: String? = null,
  val offenceCode: String? = null,
  val offenceDescription: String? = null,
  val offenceDate: String? = null,
  val latestBooking: Boolean? = false,
  val sentenceStartDate: String? = null,
  val primarySentence: Boolean? = false,
)
