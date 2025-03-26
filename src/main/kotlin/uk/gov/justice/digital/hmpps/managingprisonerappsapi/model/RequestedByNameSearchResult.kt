package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import org.springframework.beans.factory.annotation.Value

interface RequestedByNameSearchResult {
  @Value("#{target.requestedBy}")
  fun getPrisonerId(): String

  @Value("#{target.requestedByFirstName}")
  fun getFirstName(): String

  @Value("#{target.requestedByLastName}")
  fun getLastName(): String
}
