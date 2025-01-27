package uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Decision
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Response
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import java.time.LocalDateTime
import java.util.*

class DataGenerator {
  companion object {
    fun generateComment(createdBy: UUID) : Comment {
      return Comment(
        UUID.randomUUID(),
        "Looks good to me to approve",
        LocalDateTime.now(),
        createdBy,
        setOf(UUID.randomUUID()),
        UUID.randomUUID()
      )
    }

    fun generateResponse(staffId: UUID) : Response {
      return Response(
        UUID.randomUUID(),
        "Pass all requirement",
        Decision.APPROVED,
        LocalDateTime.now(),
        staffId
      )
    }

    fun generateStaff() : Staff {
      return Staff(
        UUID.randomUUID(),
        "Test",
        "Staff",
        UserCategory.STAFF,
        setOf(UUID.randomUUID()),
        "Prison Warden"
      )
    }

    // fun generateApp()
  }
}