package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import java.util.*

@Service
class StaffServiceImpl : StaffService {
  override fun getStaffById(id: String): Optional<Staff> {
    val staff = Staff("staffusername", "staffUserId", "staffFirstName", "staffLastName", UserCategory.STAFF, "TEST_ESTABLISHMENT_FIRST", setOf(UUID.randomUUID()), "jobTitle", UUID.randomUUID())
    return Optional.of(staff)
  }
}
