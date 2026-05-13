package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource.sar

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
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
  @GetMapping("/{documentId}/file")
  @PreAuthorize("hasAnyRole('SAR_DATA_ACCESS', 'DOCUMENT_READER')")
  @Operation(
    summary = "Get attachment file for SAR",
    description = "Retrieves an attachment file from the Document API. Requires ROLE_SAR_DATA_ACCESS or ROLE_DOCUMENT_READER ",
    security = [SecurityRequirement(name = "bearer-jwt", scopes = ["SAR_DATA_ACCESS", "DOCUMENT_READER"])],
    responses = [
      ApiResponse(responseCode = "200", description = "File content returned", content = [Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)]),
      ApiResponse(responseCode = "401", description = "Unauthorised - valid token required"),
      ApiResponse(responseCode = "403", description = "Forbidden - SAR_DATA_ACCESS required"),
      ApiResponse(responseCode = "404", description = "Document not found"),
    ],
  )
  fun getAttachmentFile(
    @Parameter(description = "The document ID to retrieve", required = true)
    @PathVariable documentId: String,
  ): ResponseEntity<ByteArray> {
    val docResponse = documentApiClient.getDocumentFile(documentId)
    val contentType = docResponse.headers.contentType ?: MediaType.APPLICATION_OCTET_STREAM
    val disposition = docResponse.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)
    val responseBuilder = ResponseEntity.ok().contentType(contentType)
    if (!disposition.isNullOrBlank()) responseBuilder.header(HttpHeaders.CONTENT_DISPOSITION, disposition)
    return responseBuilder.body(docResponse.body)
  }
}
