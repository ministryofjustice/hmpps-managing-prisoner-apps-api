package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.integration.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException

@Component
class HmppsAuthClient(private var webClientBuilder: WebClient.Builder) {

  @Value("\${hmpps.auth.base-url}")
  private lateinit var hmppsAuthBaseUrl: String

  @Value("\${SYSTEM_CLIENT_ID}")
  private lateinit var hmppsAuthUsername: String

  @Value("\${SYSTEM_CLIENT_SECRET}")
  private lateinit var hmppsAuthPassword: String

  companion object {
    private val logger = LoggerFactory.getLogger(HmppsAuthClient::class.java)
  }

  @Cacheable("hmpps-auth-token", key = "#root.methodName")
  fun getBearerToken(): String {
    logger.info("Calling HMPPS Auth service for access token")
    try {
      val webClient = webClientBuilder
        .baseUrl(hmppsAuthBaseUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, getBasicAuthHeader())
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()
      val response = webClient.post()
        .uri("/oauth/token?grant_type=client_credentials")
        .retrieve()
        .toEntity(HmppsAuthAccessToken::class.java)
        .block()!!

      val body = response.body
      if (response.statusCode.is2xxSuccessful && body != null) {
        return "Bearer ${body.accessToken}"
      } else {
        throw ApiException(
          "Response code ${response.statusCode.value()} making request to Hmpps auth for access token",
          HttpStatus.INTERNAL_SERVER_ERROR
        )
      }
    } catch (e: WebClientResponseException) {
      throw ApiException(
        "Response code ${e.statusCode.value()} making request to Hmpps auth for access token",
        HttpStatus.INTERNAL_SERVER_ERROR
      )
    }
  }

  private fun getBasicAuthHeader(): String {
    val encoder = Base64.getEncoder()
    val authCode = String(encoder.encode("$hmppsAuthUsername:$hmppsAuthPassword".toByteArray(Charsets.UTF_8)))
    return "Basic $authCode"
  }
}
