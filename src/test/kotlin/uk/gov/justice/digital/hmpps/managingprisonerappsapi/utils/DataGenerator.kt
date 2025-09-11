package uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils

import com.fasterxml.uuid.Generators
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Decision
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Groups
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Response
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class DataGenerator {
  companion object {
    val MESSAGE = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit." +
      " Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes," +
      " nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis," +
      " sem. Nulla consequat massa quis enim. Donec pede justo, fringilla vel," +
      " aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet a," +
      " venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt." +
      " Cras dapibus. Vivamus elementum semper nisi. Aenean vulputate eleifend tellus. Aenean" +
      " leo ligula, porttitor eu, consequat vitae, eleifend ac, enim. Aliquam lorem ante, dapibus in," +
      " viverra quis, feugiat a, tellus. Phasellus viverra nulla ut metus varius laoreet. Quisque rutrum." +
      " Aenean imperdiet. Etiam ultricies nisi vel augue. Curabitur ullamcorper" +
      " ultricies nisi. Nam eget dui. Etiam rhoncus. Maecenas tempus, tellus eget" +
      " condimentum rhoncus, sem quam semper libero, sit amet adipiscing sem swneque sed ipsum."

    val CONTACT_NUMBER = "1234567890"
    fun generateComment(createdBy: String): Comment = Comment(
      Generators.timeBasedEpochGenerator().generate(),
      MESSAGE,
      LocalDateTime.now(),
      createdBy,
      UUID.randomUUID(),
    )

    fun generateResponse(staffId: String): Response = Response(
      Generators.timeBasedEpochGenerator().generate(),
      "Pass all requirement",
      Decision.APPROVED,
      LocalDateTime.now(),
      staffId,
    )

    fun generateApp(): App = App(
      Generators.timeBasedEpochGenerator().generate(),
      UUID.randomUUID().toString(),
      Generators.timeBasedEpochGenerator().generate(),
      AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT,
      LocalDateTime.now(),
      LocalDateTime.now(),
      "testStaaf@moj",
      LocalDateTime.now(),
      "testStaaf@moj",
      arrayListOf(Generators.timeBasedEpochGenerator().generate()),
      listOf(HashMap<String, Any>().apply { put("contact", 123456) }),
      "testprisoner@moj",
      "Test",
      "Prisoner",
      AppStatus.PENDING,
      Generators.timeBasedEpochGenerator().generate().toString(),
      mutableListOf(),
      false,
    )

    fun generateApp(
      establishmentId: String,
      appType: AppType,
      requestedBy: String,
      requestedDate: LocalDateTime,
      requestedByFirstName: String,
      requestedByLastName: String,
      appStatus: AppStatus,
      groupId: UUID,
      firstNightCenter: Boolean,
    ): App = App(
      Generators.timeBasedEpochGenerator().generate(),
      UUID.randomUUID().toString(),
      groupId,
      appType,
      requestedDate,
      LocalDateTime.now(ZoneOffset.UTC),
      "testStaaf@moj",
      LocalDateTime.now(ZoneOffset.UTC),
      "testStaaf@moj",
      arrayListOf(Generators.timeBasedEpochGenerator().generate()),
      listOf(HashMap<String, Any>().apply { put("contact", 123456) }),
      requestedBy,
      requestedByFirstName,
      requestedByLastName,
      appStatus,
      establishmentId,
      mutableListOf(),
      firstNightCenter,
    )

    fun generateEstablishment(): Establishment = Establishment(
      "HST",
      "Test Establishment",
      AppType.entries.toSet(),
      false
    )

    fun generateGroups(
      id: UUID,
      establishmentId: String,
      groupName: String,
      initialApps: List<AppType>,
      groupType: GroupType,
    ): Groups = Groups(
      id,
      groupName,
      establishmentId,
      initialApps,
      groupType,
    )

    fun generateAppRequestDto(
      appType: AppType,
      requestedDate: LocalDateTime?,
      requestedByFirstName: String,
      requestedByLastName: String,
      departmentId: UUID?,
    ): AppRequestDto = AppRequestDto(
      "Testing",
      appType.toString(),
      requestedDate,
      listOf(
        HashMap<String, Any>()
          .apply {
            // put("amount", 10)
            put("contact-number", CONTACT_NUMBER)
            // put("firstName", "John")
            // put("lastName", "Smith")
          },
      ),
      null,
      departmentId,
    )
  }
}
