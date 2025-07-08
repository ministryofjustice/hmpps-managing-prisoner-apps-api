package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import java.util.*

@Service
class StaffServiceImpl(
  var manageUsersApiClient: ManageUsersApiClient,
) : StaffService {
  @Value("\${spring.profiles.active:test}")
  private lateinit var activeProfile: String

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun getStaffById(staffId: String): Optional<Staff> {
    val userDetailsDto = manageUsersApiClient.getUserDetails(staffId)
    val staff = Staff(
      userDetailsDto.username,
      userDetailsDto.userId,
      userDetailsDto.fullName,
      UserCategory.STAFF,
      if (activeProfile.equals("test")) "TEST_ESTABLISHMENT_FIRST" else userDetailsDto.activeCaseLoadId,
      // setOf(UUID.randomUUID()), // Currently role is not required.
      "staffJobTitle", // To-Do find endpoint to get job title of the staff.
      userDetailsDto.uuid,
    )
    logger.info("Staff detail id: ${staff.username} and active caseload id: ${staff.establishmentId}")

    return Optional.of(staff)
  }
}
