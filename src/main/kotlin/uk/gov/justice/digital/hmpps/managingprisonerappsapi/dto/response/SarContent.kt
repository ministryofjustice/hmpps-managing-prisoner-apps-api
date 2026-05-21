package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Decision
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
  val type: String,
  val requestedDate: LocalDateTime,
  val lastModifiedDate: LocalDateTime,
  val establishment: String,
  val assignedTo: String,
  val history: List<PrnAppHistory>,
  // val formData: List<Map<String, Any>>,
  val formDataItems: List<FormDataItem>,
  val comments: List<PrnAppComment>,
  val responses: List<PrnAppResponse>,
  val appAttachments: List<PrnAppAttachment>,
)

data class PrnAppHistory(
  val activity: String,
  val date: LocalDateTime,
  val actionBy: String,
)

data class FormDataItem(
  val key: String,
  val value: String,
)

data class PrnAppComment(
  val message: String,
  val createdBy: String,
  val createdDateTime: LocalDateTime,
)

data class PrnAppResponse(
  val decision: Decision,
  val createdBy: String,
  val reason: String,
)

data class PrnAppAttachment(
  val contentType: String,
  val url: String,
  val headers: List<AttachmentHeader>,
  @JsonIgnore val documentId: String,
)

data class AttachmentHeader(
  val name: String,
  val value: String,
)
