package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request

import java.util.UUID

data class FileRequestDto(
  val documentId: UUID,
  val fileName: String,
  val fileType: String,
)
