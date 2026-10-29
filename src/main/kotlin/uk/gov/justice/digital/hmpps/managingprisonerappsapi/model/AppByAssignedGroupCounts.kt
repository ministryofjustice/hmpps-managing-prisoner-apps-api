package uk.gov.justice.digital.hmpps.managingprisonerappsapi.model

import org.springframework.beans.factory.annotation.Value

interface AppByAssignedGroupCounts {
  @Value("#{target.count}")
  fun getCount(): Int

  @Value("#{target.assignedGroup}")
  fun getAssignedGroup(): String
}
