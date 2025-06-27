package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import java.time.LocalDateTime
import java.util.UUID

data class SarContent(
  val firstName: String,
  val lastName: String,
  val prisonerId: String,
  val apps: List<PrnApp>,
)

data class PrnApp(
  val id: UUID,
  val status: AppStatus,
  val type: AppType,
  val requestedDate: LocalDateTime,
  val lastModifiedDate: LocalDateTime,
  val establishment: String,
  val history: List<PrnAppHistory>,
  val formData: List<Map<String, Any>>,
)

data class PrnAppHistory(
  val activity: String,
  val date: LocalDateTime,
  val actionBy: String,
)
