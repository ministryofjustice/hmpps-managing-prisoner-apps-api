package uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Decision
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Response
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import java.time.LocalDateTime
import java.util.*

class DataGenerator {
  companion object {
    fun generateComment(createdBy: UUID): Comment {
      return Comment(
        UUID.randomUUID(),
        "Looks good to me to approve",
        LocalDateTime.now(),
        createdBy,
        setOf(UUID.randomUUID()),
        UUID.randomUUID(),
      )
    }

    fun generateResponse(staffId: UUID): Response {
      return Response(
        UUID.randomUUID(),
        "Pass all requirement",
        Decision.APPROVED,
        LocalDateTime.now(),
        staffId,
      )
    }

    fun generateStaff(): Staff {
      return Staff(
        UUID.randomUUID(),
        "Test",
        "Staff",
        UserCategory.STAFF,
        setOf(UUID.randomUUID()),
        "Prison Warden",
      )
    }

    fun generateApp(): App {
      return App(
        UUID.randomUUID(),
        UUID.randomUUID().toString(),
        UUID.randomUUID(),
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        LocalDateTime.now(),
        LocalDateTime.now(),
        UUID.randomUUID(),
        arrayListOf(UUID.randomUUID()),
        listOf(HashMap<String, Any>().apply { put("contact", 123456) }),
        LocalDateTime.now(),
        UUID.randomUUID(),
        UUID.randomUUID(),
      )
    }

    fun generateAppRequestDto(): AppRequestDto {
      return AppRequestDto(
        "Testing",
        AppType.PIN_PHONE_ADD_NEW_CONTACT.toString(),
        LocalDateTime.now(),
        listOf(
          HashMap<String, Any>()
            .apply {
              put("amount", 10)
              put("contact-number", "234567")
              put("firstName", "John")
              put("lastName", "Smith")
            },
        ),
      )
    }
  }
}
