package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.integration.client.LocationsInsidePrisonApiClient

@Service
class LocationServiceImpl(
  private val locationsInsidePrisonApiClient: LocationsInsidePrisonApiClient,
) : LocationService {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun getPrisonersAtLocations(locationIds: Set<String>): Set<String> {
    if (locationIds.isEmpty()) {
      return emptySet()
    }

    val allPrisoners = mutableSetOf<String>()

    locationIds.forEach { locationId ->
      try {
        val locations = locationsInsidePrisonApiClient.getPrisonersAtLocation(locationId)
        locations.forEach { locationCell ->
          locationCell.prisoners.forEach { prisoner ->
            allPrisoners.add(prisoner.prisonerNumber)
          }
        }
        logger.info("Found ${allPrisoners.size} prisoners at location: $locationId")
      } catch (e: Exception) {
        logger.warn("Error getting prisoners for location: $locationId", e)
      }
    }

    logger.info("Total prisoners from ${locationIds.size} locations: ${allPrisoners.size}")
    return allPrisoners
  }
}
