package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import org.springframework.beans.factory.annotation.Value

interface AppByFirstNightCount {
  @Value("#{target.count}")
  fun getCount(): Int
}