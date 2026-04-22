package uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils

import com.fasterxml.uuid.Generators
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppRequestPrisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.FileRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Decision
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Groups
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Response
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.SubmittedByType
import java.time.LocalDateTime
import java.util.*

class DataGenerator {
  companion object {

    private const val OLD_NOMS_NUMBER = "A1234AA"
    private const val NEW_NOMS_NUMBER = "B1234BB"

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
    val assignedGroup = UUID.fromString("22222222-2222-2222-2222-222222222222")

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
      null,
      null,
      requestedDate = LocalDateTime.now(),
      createdDate = LocalDateTime.now(),
      createdBy = "testStaaf@moj",
      submittedByType = SubmittedByType.STAFF,
      lastModifiedDate = LocalDateTime.now(),
      lastModifiedBy = "testStaaf@moj",
      comments = arrayListOf(Generators.timeBasedEpochGenerator().generate()),
      requests = listOf(HashMap<String, Any>().apply { put("contact", 123456) }),
      requestedBy = "testprisoner@moj",
      requestedByFirstName = "Test",
      requestedByLastName = "Prisoner",
      status = AppStatus.PENDING,
      establishmentId = Generators.timeBasedEpochGenerator().generate().toString(),
      responses = mutableListOf(),
      firstNightCenter = false,
    )

    fun generateApp(
      establishmentId: String,
      appType: AppType?,
      applicationType: Long?,
      applicationGroup: Long?,
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
      applicationGroup,
      applicationType,
      requestedDate = requestedDate,
      createdDate = requestedDate,
      createdBy = "testStaaf@moj",
      submittedByType = SubmittedByType.STAFF,
      lastModifiedDate = requestedDate,
      lastModifiedBy = "testStaaf@moj",
      comments = arrayListOf(),
      requests = listOf(HashMap<String, Any>().apply { put("contact", 123456) }),
      requestedBy = requestedBy,
      requestedByFirstName = requestedByFirstName,
      requestedByLastName = requestedByLastName,
      status = appStatus,
      establishmentId = establishmentId,
      responses = mutableListOf(),
      firstNightCenter = firstNightCenter,
    )

    fun generateEstablishment(): Establishment = Establishment(
      "HST",
      "Test Establishment",
      AppType.entries.toSet(),
      false,
      setOf(),
      setOf(),
    )

    fun generateGroups(
      id: UUID,
      establishmentId: String,
      groupName: String,
      initialApps: List<Long>,
      groupType: GroupType,
    ): Groups = Groups(
      id,
      groupName,
      establishmentId,
      listOf(),
      initialApps,
      groupType,
    )

    fun generateAppRequestDto(
      appType: AppType?,
      applicationType: Long?,
      genericForm: Boolean,
      applicationGroup: Long?,
      requestedDate: LocalDateTime?,
      requestedByFirstName: String,
      requestedByLastName: String,
      departmentId: UUID?,
      files: List<FileRequestDto>,
    ): AppRequestDto = AppRequestDto(
      "Testing",
      appType.toString(),
      applicationType,
      genericForm,
      applicationGroup,
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
      files,
    )

    fun generateAppForMerge(
      id: UUID,
      reference: String,
      assignedGroup: UUID,
      appType: AppType,
      applicationGroup: Long,
      applicationType: Long,
      requestedDate: LocalDateTime,
      requestedBy: String,
      requestedByFirstName: String,
      requestedByLastName: String,
      status: AppStatus,
      establishmentId: String,
      firstNightCenter: Boolean,
    ): App = App(
      id,
      reference,
      assignedGroup,
      appType,
      applicationGroup,
      applicationType,
      genericForm = false,
      requestedDate = requestedDate,
      createdDate = requestedDate,
      createdBy = "TEST_USER",
      submittedByType = SubmittedByType.STAFF,
      lastModifiedDate = requestedDate,
      lastModifiedBy = "TEST_USER",
      comments = mutableListOf(),
      requests = listOf(HashMap<String, Any>().apply { put("contact", 123456) }),
      requestedBy = requestedBy,
      requestedByFirstName = requestedByFirstName,
      requestedByLastName = requestedByLastName,
      status = status,
      establishmentId = establishmentId,
      responses = mutableListOf(),
      firstNightCenter = firstNightCenter,
    )

    fun generateNewNomsMergeApp(): App = generateAppForMerge(
      id = UUID.fromString("11111111-1111-1111-1111-111111111114"),
      reference = "REF-004",
      assignedGroup = assignedGroup,
      appType = AppType.PIN_PHONE_REMOVE_CONTACT,
      applicationGroup = 1,
      applicationType = 1,
      requestedDate = LocalDateTime.of(2026, 1, 4, 10, 0, 0),
      requestedBy = NEW_NOMS_NUMBER,
      requestedByFirstName = "Jane",
      requestedByLastName = "Smith",
      status = AppStatus.PENDING,
      establishmentId = "MDI",
      firstNightCenter = false,
    )

    fun generateOldNomsMergeApp1(): App = generateAppForMerge(
      id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
      reference = "REF-001",
      assignedGroup = assignedGroup,
      appType = AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT,
      applicationGroup = 1,
      applicationType = 1,
      requestedDate = LocalDateTime.of(2026, 1, 1, 10, 0, 0),
      requestedBy = OLD_NOMS_NUMBER,
      requestedByFirstName = "John",
      requestedByLastName = "Doe",
      status = AppStatus.PENDING,
      establishmentId = "MDI",
      firstNightCenter = false,
    )

    fun generateOldNomsMergeApp2(): App = generateAppForMerge(
      id = UUID.fromString("11111111-1111-1111-1111-111111111112"),
      reference = "REF-002",
      assignedGroup = assignedGroup,
      appType = AppType.PIN_PHONE_EMERGENCY_CREDIT_TOP_UP,
      applicationGroup = 1,
      applicationType = 2,
      requestedDate = LocalDateTime.of(2026, 1, 2, 10, 0, 0),
      requestedBy = OLD_NOMS_NUMBER,
      requestedByFirstName = "John",
      requestedByLastName = "Doe",
      status = AppStatus.DECLINED,
      establishmentId = "MDI",
      firstNightCenter = false,
    )

    fun generateOldNomsMergeApp3(): App = generateAppForMerge(
      id = UUID.fromString("11111111-1111-1111-1111-111111111113"),
      reference = "REF-003",
      assignedGroup = assignedGroup,
      appType = AppType.PIN_PHONE_ADD_NEW_OFFICIAL_CONTACT,
      applicationGroup = 1,
      applicationType = 1,
      requestedDate = LocalDateTime.of(2026, 1, 3, 10, 0, 0),
      requestedBy = OLD_NOMS_NUMBER,
      requestedByFirstName = "John",
      requestedByLastName = "Doe",
      status = AppStatus.APPROVED,
      establishmentId = "MDI",
      firstNightCenter = true,
    )
  }

  fun generateAppRequestPrisonerFacing(applicationType: Long, genericForm: Boolean): AppRequestPrisoner = AppRequestPrisoner(
    UUID.randomUUID().toString(),
    applicationType,
    genericForm,
    listOf(
      HashMap<String, Any>()
        .apply {
          // put("amount", 10)
          put("contact-number", CONTACT_NUMBER)
          // put("firstName", "John")
          // put("lastName", "Smith")
        },
    ),
  )
}
