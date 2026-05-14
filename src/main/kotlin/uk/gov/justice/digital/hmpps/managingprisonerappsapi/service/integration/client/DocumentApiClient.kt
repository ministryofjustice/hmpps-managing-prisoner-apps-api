package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.integration.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@Component
class DocumentApiClient(
  @Qualifier("documentApiWebClient") private val webClient: WebClient,
  @Value("\${hmpps.document.api.timeout:30s}") private val apiTimeout: Duration,
  @Value("\${hmpps.service.name}") private val serviceName: String,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Fetches the raw file bytes for a document from Document API.
   * The OAuth2 bearer token is injected automatically by the authorisedWebClient.
   * Returns the full ResponseEntity so callers can forward content-type and
   * content-disposition headers from Document API.
   */
  fun getDocumentFile(documentId: String): ResponseEntity<ByteArray> {
    log.debug("Fetching document file for documentId={}", documentId)
    return webClient.get()
      .uri("/documents/{documentId}/file", documentId)
      .header("Service-Name", serviceName)
      .retrieve()
      .toEntity(ByteArray::class.java)
      .block(apiTimeout)
      ?: throw IllegalStateException("No content returned from Document API for documentId=$documentId")
  }
}
