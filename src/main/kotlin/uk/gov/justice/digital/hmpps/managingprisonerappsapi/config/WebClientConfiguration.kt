package uk.gov.justice.digital.hmpps.managingprisonerappsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

private const val MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024

@Configuration
class WebClientConfiguration(
  @Value("\${example-api.url}") val exampleApiBaseUri: String,
  @Value("\${hmpps.auth.base-url}") val hmppsAuthBaseUri: String,
  @Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @Value("\${api.timeout:10s}") val apiTimeout: Duration,
  @Value("\${hmpps.prisoner-search.api.url}")
  private val prisonSearchBaseUrl: String,
  @Value("\${hmpps.manage-users.api.url}")
  private val manageUsersApiBaseUrl: String,
  @Value("\${hmpps.document.api.url}")
  private val documentApiBaseUrl: String,
) {
  private enum class HmppsAuthClientRegistrationId(val clientRegistrationId: String) {
    PRISONER_SEARCH("other-hmpps-apis"),
    MANAGE_USERS_API_CLIENT("other-hmpps-apis"),
    DOCUMENT_API("other-hmpps-apis"),
  }

  @Bean
  fun documentApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient {
    val largeBufferBuilder = builder.clone().exchangeStrategies(
      ExchangeStrategies.builder()
        .codecs { it.defaultCodecs().maxInMemorySize(MAX_ATTACHMENT_SIZE) }
        .build(),
    )
    return largeBufferBuilder.authorisedWebClient(authorizedClientManager, registrationId = HmppsAuthClientRegistrationId.DOCUMENT_API.clientRegistrationId, url = documentApiBaseUrl, apiTimeout)
  }

  @Bean
  fun prisonerSearchWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = HmppsAuthClientRegistrationId.PRISONER_SEARCH.clientRegistrationId, url = prisonSearchBaseUrl, apiTimeout)

  @Bean
  fun manageUsersApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = HmppsAuthClientRegistrationId.MANAGE_USERS_API_CLIENT.clientRegistrationId, url = manageUsersApiBaseUrl, apiTimeout)

  // HMPPS Auth health ping is required if your service calls HMPPS Auth to get a token to call other services
  // TODO: Remove the health ping if no call outs to other services are made
  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  // TODO: This is an example health bean for checking other services and should be removed / replaced
  @Bean
  fun exampleApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(exampleApiBaseUri, healthTimeout)

  // TODO: This is an example bean for calling other services and should be removed / replaced
  @Bean
  fun exampleApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "example-api", url = exampleApiBaseUri, apiTimeout)
}
