package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.integration.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import java.time.Duration
import java.util.Base64

@Component
class DocumentApiClient(
  @Qualifier("documentApiWebClient") private val webClient: WebClient,
  private val hmppsAuthClient: HmppsAuthClient,
  @Value("\${hmpps.document.api.timeout:30s}") private val apiTimeout: Duration,
  @Value("\${hmpps.service.name}") private val serviceName: String,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val REQUIRED_ROLE = "ROLE_DOCUMENT_READER"
    private val objectMapper = ObjectMapper()
  }

  /**
   * Fetches the raw file bytes for a document from Document API.
   * GET /documents/{documentId}/file
   * - Authorization: Bearer token obtained from HmppsAuthClient
   * - Validates the token has ROLE_DOCUMENT_READER before invoking Document API
   */
  fun getDocumentFile(documentId: String): ByteArray {
    log.info("Fetching document file for documentId={}", documentId)

    val bearerToken = hmppsAuthClient.getBearerToken()
    validateTokenHasDocumentReaderRole(bearerToken)

    return webClient.get()
      .uri("/documents/{documentId}/file", documentId)
      .accept(MediaType.APPLICATION_OCTET_STREAM)
      .header(HttpHeaders.AUTHORIZATION, bearerToken)
      .header("Service-Name", serviceName)
      .retrieve()
      .bodyToFlux(DataBuffer::class.java)
      .collectList()
      .map { buffers ->
        val totalSize = buffers.sumOf { it.readableByteCount() }
        val bytes = ByteArray(totalSize)
        var offset = 0
        buffers.forEach { buf ->
          val readable = buf.readableByteCount()
          buf.read(bytes, offset, readable)
          offset += readable
        }
        bytes
      }
      .block(apiTimeout)
      ?: throw IllegalStateException("No content returned from Document API for documentId=$documentId")
  }

  /**
   * check that ROLE_DOCUMENT_READER is present in the authorities.
   * Throws ApiException(403) if the role is missing.
   */
  private fun validateTokenHasDocumentReaderRole(bearerToken: String) {
    try {
      val jwt = bearerToken.removePrefix("Bearer ").trim()
      val payloadJson = String(Base64.getUrlDecoder().decode(jwt.split(".")[1]))
      val payload = objectMapper.readTree(payloadJson)

      val authorities = payload.path("authorities")
        .map { it.asText() }

      if (!authorities.contains(REQUIRED_ROLE)) {
        log.warn("Client token is missing $REQUIRED_ROLE.")
        throw ApiException(
          "Client does not have the required role to access Document API",
          HttpStatus.FORBIDDEN,
        )
      }
    } catch (e: ApiException) {
      throw e
    } catch (e: Exception) {
      log.error("Failed to decode/validate bearer token roles", e)
      throw ApiException("Failed to validate Document API token permissions", HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }
}
