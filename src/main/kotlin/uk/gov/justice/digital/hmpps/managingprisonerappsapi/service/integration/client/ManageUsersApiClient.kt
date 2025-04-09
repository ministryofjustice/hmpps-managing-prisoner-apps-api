package uk.gov.justice.digital.hmpps.managingprisonerappsapi.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.manage.users.UserDetailsDto
import java.time.Duration

@Component
class ManageUsersApiClient(
  @Qualifier("manageUsersApiWebClient") private val webClient: WebClient,
  @Value("\${hmpps.manage-users.api.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }
  fun getUserDetails(userName: String): UserDetailsDto {
    LOG.info("Fetching staff details for user {}", userName)
    return webClient.get()
      .uri("/users/$userName")
      .retrieve()
      .bodyToMono<UserDetailsDto>()
      .onErrorResume { e ->
        e.printStackTrace()
        if (e is WebClientResponseException) {
          LOG.warn("Failed to acquire user information from hmpps-manage-users-api $userName ", e)
          //   return@onErrorResume Mono.just(UserDetailsDto(userName))
        }
        LOG.error("Failed to acquire user information from hmpps-manage-users-api $userName ", e)
        Mono.error(e)
      }.blockOptional(apiTimeout).get()
  }
}
