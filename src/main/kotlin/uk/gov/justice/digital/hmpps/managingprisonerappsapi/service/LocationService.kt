package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

interface LocationService {

  fun getPrisonersAtLocations(locationIds: Set<String>): Set<String>
}
