package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.CommentRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AssignedGroupDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Groups
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator.Companion.CONTACT_NUMBER
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class AppServiceImplTest {

  private val establishmentId = "TE1"
  private val requestedBy = "A12345"
  private val requestedByFirstName = "Test"
  private val requestedByLastName = "Prisoner"
  private val createdBy = "Staff12345"
  private val staffFullName = "Test Staff"
  private val groupId = UUID.randomUUID()
  private val forwardingComment = "Forwarding Comment"

  private lateinit var prisoner: Prisoner
  private lateinit var staff: Staff
  private lateinit var appRepository: AppRepository
  private lateinit var prisonerService: PrisonerService
  private lateinit var staffService: StaffService
  private lateinit var groupService: GroupService
  private lateinit var commentRepository: CommentRepository

  private lateinit var appService: AppService
  private lateinit var app: App
  private lateinit var comment: Comment

  @BeforeEach
  fun beforeEach() {
    appRepository = Mockito.mock(AppRepository::class.java)
    prisonerService = Mockito.mock(PrisonerService::class.java)
    staffService = Mockito.mock(StaffService::class.java)
    groupService = Mockito.mock(GroupService::class.java)
    commentRepository = Mockito.mock(CommentRepository::class.java)

    app = DataGenerator.generateApp(
      establishmentId,
      AppType.PIN_PHONE_ADD_NEW_CONTACT,
      requestedBy,
      LocalDateTime.now(ZoneOffset.UTC),
      requestedByFirstName,
      requestedByLastName,
      AppStatus.PENDING,
      groupId,
    )

    comment = Comment(
      UUID.randomUUID(),
      forwardingComment,
      LocalDateTime.now(ZoneOffset.UTC),
      app.createdBy,
      app.id,
    )

    staff = Staff(
      createdBy,
      UUID.randomUUID().toString(),
      staffFullName,
      UserCategory.STAFF,
      establishmentId,
      "Staff",
      UUID.randomUUID(),
    )
    prisoner = Prisoner(
      requestedBy,
      UUID.randomUUID().toString(),
      requestedByFirstName,
      requestedByLastName,
      UserCategory.PRISONER,
      UUID.randomUUID().toString(),
      UUID.randomUUID().toString(),
      establishmentId,
    )

    appService = AppServiceImpl(appRepository, prisonerService, staffService, groupService, commentRepository)
  }

  @Test
  fun `submit App when prisoner and staff establishment do not match`() {
    Mockito.`when`(appRepository.save(app)).thenReturn(app)
    Mockito.`when`(staffService.getStaffById(createdBy)).thenReturn(
      Optional.of(
        Staff(
          createdBy,
          UUID.randomUUID().toString(),
          staffFullName,
          UserCategory.STAFF,
          establishmentId + "Test",
          "Staff",
          UUID.randomUUID(),
        ),
      ),
    )
    Mockito.`when`(prisonerService.getPrisonerById(requestedBy)).thenReturn(
      Optional.of(prisoner),
    )
    val exception = assertThrows(ApiException::class.java) {
      appService.submitApp(
        requestedBy,
        createdBy,
        DataGenerator.generateAppRequestDto(
          AppType.PIN_PHONE_ADD_NEW_CONTACT,
          LocalDateTime.now(ZoneOffset.UTC),
          requestedByFirstName,
          requestedByLastName,
        ),
      )
    }
    assertEquals(HttpStatus.FORBIDDEN, exception.status)
  }

  @Test
  fun `submit App when prisoner do not found`() {
    Mockito.`when`(appRepository.save(app)).thenReturn(app)
    Mockito.`when`(staffService.getStaffById(createdBy)).thenReturn(
      Optional.of(staff),
    )
    Mockito.`when`(prisonerService.getPrisonerById(requestedBy)).thenReturn(
      Optional.empty(),
    )
    val exception = assertThrows(ApiException::class.java) {
      appService.submitApp(
        requestedBy,
        createdBy,
        DataGenerator.generateAppRequestDto(
          AppType.PIN_PHONE_ADD_NEW_CONTACT,
          LocalDateTime.now(ZoneOffset.UTC),
          requestedByFirstName,
          requestedByLastName,
        ),
      )
    }
    assertEquals(HttpStatus.NOT_FOUND, exception.status)
  }

  @Test
  fun `submit App when staff do not found`() {
    Mockito.`when`(appRepository.save(app)).thenReturn(app)
    Mockito.`when`(staffService.getStaffById(createdBy)).thenReturn(
      Optional.empty(),
    )
    Mockito.`when`(prisonerService.getPrisonerById(requestedBy)).thenReturn(
      Optional.of(prisoner),
    )
    val exception = assertThrows(ApiException::class.java) {
      appService.submitApp(
        requestedBy,
        createdBy,
        DataGenerator.generateAppRequestDto(
          AppType.PIN_PHONE_ADD_NEW_CONTACT,
          LocalDateTime.now(ZoneOffset.UTC),
          requestedByFirstName,
          requestedByLastName,
        ),
      )
    }
    assertEquals(HttpStatus.NOT_FOUND, exception.status)
  }

  @Test
  fun `submit App`() {
    Mockito.`when`(staffService.getStaffById(createdBy)).thenReturn(
      Optional.of(staff),
    )
    Mockito.`when`(prisonerService.getPrisonerById(requestedBy)).thenReturn(
      Optional.of(prisoner),
    )
    Mockito.`when`(groupService.getGroupByInitialAppType(establishmentId, app.appType)).thenReturn(
      Groups(
        groupId,
        "Test Group",
        establishmentId,
        listOf(AppType.PIN_PHONE_ADD_NEW_CONTACT),
        GroupType.WING,
      ),
    )
    Mockito.`when`(groupService.getGroupById(groupId)).thenReturn(
      AssignedGroupDto(
        groupId,
        "Test Group",
        EstablishmentDto(
          establishmentId,
          "Test Establishment",
        ),
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        GroupType.WING,
      ),
    )
    Mockito.`when`(appRepository.save(any())).thenReturn(app)
    val appResponse = appService.submitApp(
      requestedBy,
      createdBy,
      DataGenerator.generateAppRequestDto(
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        LocalDateTime.now(ZoneOffset.UTC),
        requestedByFirstName,
        requestedByLastName,
      ),
    )
    assertApp(app, appResponse)
  }

  @Test
  fun `submit App when request size is 0 or more than 1`() {
    Mockito.`when`(staffService.getStaffById(createdBy)).thenReturn(
      Optional.of(staff),
    )
    Mockito.`when`(prisonerService.getPrisonerById(requestedBy)).thenReturn(
      Optional.of(prisoner),
    )
    Mockito.`when`(groupService.getGroupByInitialAppType(establishmentId, app.appType)).thenReturn(
      Groups(
        groupId,
        "Test Group",
        establishmentId,
        listOf(AppType.PIN_PHONE_ADD_NEW_CONTACT),
        GroupType.WING,
      ),
    )
    Mockito.`when`(groupService.getGroupById(groupId)).thenReturn(
      AssignedGroupDto(
        groupId,
        "Test Group",
        EstablishmentDto(
          establishmentId,
          "Test Establishment",
        ),
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        GroupType.WING,
      ),
    )
    Mockito.`when`(appRepository.save(any())).thenReturn(app)
    var exception = assertThrows(ApiException::class.java) {
      appService.submitApp(
        requestedBy,
        createdBy,
        AppRequestDto(
          "Testing",
          AppType.PIN_PHONE_ADD_NEW_CONTACT.toString(),
          LocalDateTime.now(ZoneOffset.UTC),
          listOf(),
        ),
      )
    }
    assertEquals(HttpStatus.BAD_REQUEST, exception.status)

    exception = assertThrows(ApiException::class.java) {
      appService.submitApp(
        requestedBy,
        createdBy,
        AppRequestDto(
          "Testing",
          AppType.PIN_PHONE_ADD_NEW_CONTACT.toString(),
          LocalDateTime.now(ZoneOffset.UTC),
          listOf(
            HashMap<String, Any>()
              .apply {
                // put("amount", 10)
                put("contact-number", CONTACT_NUMBER)
                // put("firstName", "John")
                // put("lastName", "Smith")
              },
            HashMap<String, Any>()
              .apply {
                put("contact-number", CONTACT_NUMBER)
              },
          ),
        ),
      )
    }
    assertEquals(HttpStatus.BAD_REQUEST, exception.status)
  }

  @Test
  fun `get App by id`() {
    Mockito.`when`(appRepository.findAppsByIdAndRequestedBy(app.id, requestedBy)).thenReturn(Optional.of(app))
    Mockito.`when`(staffService.getStaffById(createdBy)).thenReturn(
      Optional.of(staff),
    )
    Mockito.`when`(prisonerService.getPrisonerById(requestedBy)).thenReturn(
      Optional.of(prisoner),
    )
    Mockito.`when`(groupService.getGroupById(app.assignedGroup)).thenReturn(
      AssignedGroupDto(
        groupId,
        "Test Group",
        EstablishmentDto(
          establishmentId,
          "Test Establishment",
        ),
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        GroupType.WING,
      ),
    )
    var appResponse = appService.getAppsById(
      requestedBy,
      app.id,
      createdBy,
      false,
      false,
    )
    assertApp(app, appResponse)

    appResponse = appService.getAppsById(
      requestedBy,
      app.id,
      createdBy,
      true,
      true,
    )
    assertInstanceOf(AssignedGroupDto::class.java, appResponse.assignedGroup)
    assertInstanceOf(Prisoner::class.java, appResponse.requestedBy)
    assertApp(app, appResponse)
  }

  @Test
  fun `get App by id when staff not found`() {
    Mockito.`when`(appRepository.findAppsByIdAndRequestedBy(app.id, requestedBy)).thenReturn(Optional.of(app))
    Mockito.`when`(staffService.getStaffById(createdBy)).thenReturn(
      Optional.empty(),
    )
    val exception = assertThrows(ApiException::class.java) {
      appService.getAppsById(
        requestedBy,
        app.id,
        createdBy,
        false,
        false,
      )
    }
    assertEquals(HttpStatus.FORBIDDEN, exception.status)
  }

  @Test
  fun `forward app to group when staff not found`() {
    Mockito.`when`(staffService.getStaffById(any())).thenReturn(Optional.empty())
    val exception = assertThrows(ApiException::class.java) {
      appService.forwardAppToGroup(createdBy, UUID.randomUUID(), UUID.randomUUID(), CommentRequestDto(forwardingComment))
    }
    assertEquals(HttpStatus.FORBIDDEN, exception.status)
  }

  @Test
  fun `forward app to group`() {
    val forwardGroupId = UUID.randomUUID()
    Mockito.`when`(staffService.getStaffById(createdBy)).thenReturn(Optional.of(staff))
    Mockito.`when`(appRepository.findById(app.id)).thenReturn(Optional.of(app))
    Mockito.`when`(groupService.getGroupById(groupId)).thenReturn(AssignedGroupDto(forwardGroupId, "Forward group", EstablishmentDto(establishmentId, "Test Establishment"), AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS, GroupType.WING))
    Mockito.`when`(commentRepository.save(any())).thenReturn(comment)
    val appResponse = appService.forwardAppToGroup(createdBy, forwardGroupId, app.id, CommentRequestDto(forwardingComment))
    assertEquals(forwardGroupId, app.assignedGroup)
    assertApp(app, appResponse)
  }

  private fun assertApp(app: App, appResponseDto: AppResponseDto<Any, Any>) {
    assertEquals(app.id, appResponseDto.id)
    assertEquals(app.appType, appResponseDto.appType)
    assertEquals(app.status, appResponseDto.status)
    assertEquals(app.requestedByFirstName, appResponseDto.requestedByFirstName)
    assertEquals(app.requestedByLastName, appResponseDto.requestedByLastName)
    assertEquals(app.createdBy, appResponseDto.createdBy)
  }
}
