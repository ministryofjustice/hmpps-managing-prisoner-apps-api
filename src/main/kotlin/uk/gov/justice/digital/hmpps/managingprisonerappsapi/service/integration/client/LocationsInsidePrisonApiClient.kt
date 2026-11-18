package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.integration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.integration.dto.locations.LocationCellDto
import java.time.Duration

@Component
class LocationsInsidePrisonApiClient(
  @Qualifier("locationsInsidePrisonWebClient") private val webClient: WebClient,
  @Value("\${hmpps.locations-inside-prison.api.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonersAtLocation(locationId: String): List<LocationCellDto> {
    return try {
      logger.info("Fetching prisoners at location: $locationId")
      val result = webClient
        .get()
        .uri("/prisoner-locations/id/{locationId}", locationId)
        .retrieve()
        .bodyToFlux<LocationCellDto>()
        .collectList()
        .blockOptional(apiTimeout)
        .orElse(emptyList())
      logger.info("Successfully fetched ${result.size} cell locations for location: $locationId")
      result
    } catch (e: Exception) {
      logger.warn("Failed to get prisoners at location: $locationId - ${e.message}", e)
      emptyList()
    }
  }
}
