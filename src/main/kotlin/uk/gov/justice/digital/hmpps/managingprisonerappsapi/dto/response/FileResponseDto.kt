package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response

import java.time.LocalDateTime
import java.util.UUID

data class FileResponseDto(
  val id: UUID,
  val documentId: UUID,
  val fileName: String,
  val createdDate: LocalDateTime,
  val createdBy: String,
  val fileType: String,
)
