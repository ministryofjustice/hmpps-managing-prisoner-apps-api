package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import java.util.*

@Service
class StaffServiceImpl : StaffService {
  override fun getStaffById(id: String): Optional<Staff> {
    // TODO("Not yet implemented")
    val staff = Staff(
      id,
      UUID.randomUUID().toString(),
      "Test",
      "Staff",
      UserCategory.STAFF,
      setOf(UUID.randomUUID()),
      "Establishment Warden",
    )
    return Optional.of(staff)
  }
}
