package uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Decision
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Groups
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Response
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class DataGenerator {
  companion object {
    fun generateComment(createdBy: UUID): Comment = Comment(
      UUID.randomUUID(),
      "Looks good to me to approve",
      LocalDateTime.now(),
      createdBy,
      setOf(UUID.randomUUID()),
      UUID.randomUUID(),
    )

    fun generateResponse(staffId: UUID): Response = Response(
      UUID.randomUUID(),
      "Pass all requirement",
      Decision.APPROVED,
      LocalDateTime.now(),
      staffId,
    )

    fun generateStaff(): Staff = Staff(
      "TEST_USER",
      "Test",
      "Test",
      "Staff",
      UserCategory.STAFF,
      "TEST_ESTABLISHMENT",
      setOf(UUID.randomUUID()),
      "Prison Warden",
    )

    fun generateApp(): App = App(
      UUID.randomUUID(),
      UUID.randomUUID().toString(),
      UUID.randomUUID(),
      AppType.PIN_PHONE_ADD_NEW_CONTACT,
      LocalDateTime.now(),
      LocalDateTime.now(),
      "testStaaf@moj",
      LocalDateTime.now(),
      "testStaaf@moj",
      arrayListOf(UUID.randomUUID()),
      listOf(HashMap<String, Any>().apply { put("contact", 123456) }),
      "testprisoner@moj",
      "Test Prisoner",
      AppStatus.PENDING,
      UUID.randomUUID().toString()
    )

    fun generateApp(establishmentId: String, appType: AppType, requestedBy: String, requestedByFullName: String, appStatus: AppStatus, groupId: UUID): App = App(
      UUID.randomUUID(),
      UUID.randomUUID().toString(),
      groupId,
      appType,
      LocalDateTime.now(ZoneOffset.UTC),
      LocalDateTime.now(ZoneOffset.UTC),
      "testStaaf@moj",
      LocalDateTime.now(ZoneOffset.UTC),
      "testStaaf@moj",
      arrayListOf(UUID.randomUUID()),
      listOf(HashMap<String, Any>().apply { put("contact", 123456) }),
      requestedBy,
      requestedByFullName,
      appStatus,
      establishmentId,
    )

    fun generateEstablishment(): Establishment = Establishment("HST", "Test Establishment")

    fun generateGroups(
      id: UUID,
      establishmentId: String,
      groupName: String,
      initialApps: List<AppType>,
      groupType: GroupType
    ): Groups = Groups(
      id,
      groupName,
      establishmentId,
      initialApps,
      groupType,
    )

    fun generateAppRequestDto(appType: AppType): AppRequestDto = AppRequestDto(
      "Testing",
      appType.toString(),
      LocalDateTime.now(),
      listOf(
        HashMap<String, Any>()
          .apply {
            // put("amount", 10)
            put("contact-number", "234567")
            // put("firstName", "John")
            // put("lastName", "Smith")
          },
      ),
    )
  }
}
