package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource.sar

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.integration.client.DocumentApiClient

@RestController
@RequestMapping("/sar/attachments", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
@Tag(name = "SAR Attachments", description = "Retrieve attachment files for Subject Access Requests")
class SarAttachmentResource(
  private val documentApiClient: DocumentApiClient,
) {

  //TODO - Remove 'MANAGING_PRISONER_APPS'
  @GetMapping("/{documentId}/file")
  @PreAuthorize("hasAnyRole('ROLE_DOCUMENT_READER', 'MANAGING_PRISONER_APPS')")
  @Operation(
    summary = "Get attachment file for SAR",
    description = "Retrieves an attachment file from the Document API. Requires ROLE_DOCUMENT_READER or MANAGING_PRISONER_APPS.",
    security = [SecurityRequirement(name = "bearer-jwt", scopes = ["ROLE_DOCUMENT_READER", "MANAGING_PRISONER_APPS"])],
    responses = [
      ApiResponse(responseCode = "200", description = "File content returned", content = [Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)]),
      ApiResponse(responseCode = "401", description = "Unauthorised - valid token required"),
      ApiResponse(responseCode = "403", description = "Forbidden - ROLE_DOCUMENT_READER required"),
      ApiResponse(responseCode = "404", description = "Document not found"),
    ],
  )
  fun getAttachmentFile(
    @Parameter(description = "The document ID to retrieve", required = true)
    @PathVariable documentId: String,
  ): ResponseEntity<ByteArray> {
    val fileBytes = documentApiClient.getDocumentFile(documentId)
    return ResponseEntity.ok()
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .contentLength(fileBytes.size.toLong())
      .body(fileBytes)
  }
}
