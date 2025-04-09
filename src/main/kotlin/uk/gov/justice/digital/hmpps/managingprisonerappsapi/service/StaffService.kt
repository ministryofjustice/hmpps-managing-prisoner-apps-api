package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import java.util.*

interface StaffService {

  fun getStaffById(staffId: String): Optional<Staff>
}
