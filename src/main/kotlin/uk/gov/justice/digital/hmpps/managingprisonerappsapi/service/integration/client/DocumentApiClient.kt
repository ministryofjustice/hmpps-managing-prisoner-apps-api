package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.integration.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@Component
class DocumentApiClient(
  @Qualifier("documentApiWebClient") private val webClient: WebClient,
  private val hmppsAuthClient: HmppsAuthClient,
  @Value("\${hmpps.document.api.timeout:30s}") private val apiTimeout: Duration,
  @Value("\${hmpps.service.name}") private val serviceName: String,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Fetches the raw file bytes for a document from Document API.
   * Returns the full ResponseEntity so callers can forward the content-type
   * and content-disposition headers returned by Document API.
   */
  fun getDocumentFile(documentId: String): ResponseEntity<ByteArray> {
    val bearerToken = hmppsAuthClient.getBearerToken()
    return webClient.get()
      .uri("/documents/{documentId}/file", documentId)
      .header(HttpHeaders.AUTHORIZATION, bearerToken)
      .header("Service-Name", serviceName)
      .retrieve()
      .toEntity(ByteArray::class.java)
      .block(apiTimeout)
      ?: throw IllegalStateException("No content returned from Document API for documentId=$documentId")
  }
}
