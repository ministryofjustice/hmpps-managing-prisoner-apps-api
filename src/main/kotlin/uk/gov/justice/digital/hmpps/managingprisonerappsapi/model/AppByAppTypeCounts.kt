package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import org.springframework.beans.factory.annotation.Value

interface AppByAppTypeCounts {
  @Value("#{target.count}")
  fun getCount(): Int

  @Value("#{target.applicationType}")
  fun getApplicationType(): Long
}
