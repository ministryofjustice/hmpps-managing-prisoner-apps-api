package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import java.util.*

@Service
class StaffServiceImpl(
  var manageUsersApiClient: ManageUsersApiClient,
) : StaffService {
  override fun getStaffById(id: String): Optional<Staff> {
    var userDetailsDto = (manageUsersApiClient.getUserDetails(id))
    val staff = Staff(
      userDetailsDto.username,
      userDetailsDto.staffId,
      userDetailsDto.fullName,
      UserCategory.STAFF,
      setOf(UUID.randomUUID()),
      userDetailsDto.activeCaseLoadId,
      userDetailsDto.uuid,
    )
    return Optional.of(staff)
  }
}
