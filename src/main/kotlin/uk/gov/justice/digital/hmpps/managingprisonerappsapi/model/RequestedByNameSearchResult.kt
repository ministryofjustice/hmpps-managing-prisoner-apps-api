package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import org.springframework.beans.factory.annotation.Value

interface RequestedByNameSearchResult {
  @Value("#{target.requestedBy}")
  fun getUsername(): String

  @Value("#{target.requestedByFullName}")
  fun getFullName(): String
}
