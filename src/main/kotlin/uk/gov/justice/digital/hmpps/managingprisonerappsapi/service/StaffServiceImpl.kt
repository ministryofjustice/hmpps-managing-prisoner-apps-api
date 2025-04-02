package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import java.util.Optional
import java.util.UUID

@Service
class StaffServiceImpl(
  var manageUsersApiClient: ManageUsersApiClient,
) : StaffService {
  override fun getStaffById(staffId: String): Optional<Staff> {
    var userDetailsDto = manageUsersApiClient.getUserDetails(staffId)
    val staff = Staff(
      userDetailsDto.username,
      userDetailsDto.userId,
      userDetailsDto.fullName,
      UserCategory.STAFF,
      userDetailsDto.activeCaseLoadId!!,
      setOf(UUID.randomUUID()),
      "staffJobTitle", // To-Do find endpoint to get job title of the staff
      userDetailsDto.uuid,
    )
    return Optional.of(staff)
  }
}
