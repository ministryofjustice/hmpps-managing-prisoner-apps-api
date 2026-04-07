package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.SarContent
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppFile
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.History
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppFileRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationTypeRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.HistoryRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ResponseRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID
class SarServiceTest {

  private val establishmentId = "MDI"
  private val prisonerNumber = "G5829VO"
  private val firstName = "Jane"
  private val lastName = "Doe"
  private val assignedGroup = UUID.randomUUID()
  private val requestedDate = LocalDateTime.of(2026, 1, 10, 10, 30)
  private val lastModifiedDate = LocalDateTime.of(2026, 1, 15, 0, 0)
  private val typeId = 1L
  private val documentApiUrl = "https://document-api-dev.hmpps.service.justice.gov.uk"
  private val serviceName = "hmpps-managing-prisoner-apps"

  private lateinit var appRepository: AppRepository
  private lateinit var applicationTypeRepository: ApplicationTypeRepository
  private lateinit var historyRepository: HistoryRepository
  private lateinit var appFileRepository: AppFileRepository
  private lateinit var commentRepository: CommentRepository
  private lateinit var responseRepository: ResponseRepository
  private lateinit var groupRepository: GroupRepository
  private lateinit var sarService: SarService

  @BeforeEach
  fun setUp() {
    appRepository = Mockito.mock(AppRepository::class.java)
    applicationTypeRepository = Mockito.mock(ApplicationTypeRepository::class.java)
    historyRepository = Mockito.mock(HistoryRepository::class.java)
    appFileRepository = Mockito.mock(AppFileRepository::class.java)
    commentRepository = Mockito.mock(CommentRepository::class.java)
    responseRepository = Mockito.mock(ResponseRepository::class.java)
    groupRepository = Mockito.mock(GroupRepository::class.java)
    appFileRepository = Mockito.mock(AppFileRepository::class.java)

    sarService = SarService(
      appRepository,
      applicationTypeRepository,
      historyRepository,
      appFileRepository,
      groupRepository,
      commentRepository,
      responseRepository,
      documentApiUrl,
      serviceName,
    )
  }

  @Test
  fun `get prison content for returns null when no apps exist`() {
    Mockito.`when`(appRepository.findAppsByRequestedBy(prisonerNumber)).thenReturn(emptyList())
    val result = sarService.getPrisonContentFor(prisonerNumber, null, null)
    assertNull(result)
  }

  @Test
  fun `get prison content for filters apps by date and builds sar content`() {
    val includedApp = DataGenerator.generateApp(
      establishmentId,
      null,
      typeId,
      1,
      prisonerNumber,
      requestedDate,
      firstName,
      lastName,
      AppStatus.PENDING,
      assignedGroup,
      false,
    ).apply {
      lastModifiedDate = this@SarServiceTest.lastModifiedDate
    }

    val excludedApp = DataGenerator.generateApp(
      establishmentId,
      null,
      typeId,
      1,
      prisonerNumber,
      LocalDateTime.of(2026, 1, 16, 8, 0),
      firstName,
      lastName,
      AppStatus.APPROVED,
      UUID.randomUUID(),
      false,
    ).apply {
      lastModifiedDate = LocalDateTime.of(2026, 1, 16, 0, 0)
    }

    val fileId = UUID.randomUUID()
    val applicationType = ApplicationType(typeId, "Add social contact", false, false, false)
    val histories = listOf(
      History(
        UUID.randomUUID(),
        includedApp.id,
        EntityType.APP,
        includedApp.id,
        Activity.APP_SUBMITTED,
        establishmentId,
        "staff-1",
        LocalDateTime.of(2026, 1, 10, 11, 0),
      ),
      History(
        UUID.randomUUID(),
        fileId,
        EntityType.FILE,
        includedApp.id,
        Activity.FILE_ADDED,
        establishmentId,
        "staff-1",
        LocalDateTime.of(2026, 1, 11, 9, 15),
      ),
      History(
        UUID.randomUUID(),
        includedApp.id,
        EntityType.APP,
        includedApp.id,
        Activity.APP_APPROVED,
        establishmentId,
        "staff-2",
        LocalDateTime.of(2026, 1, 15, 0, 0),
      ),
    )
    val appFile = AppFile(fileId, "document-1", "proof-of-id.pdf", LocalDateTime.of(2026, 1, 11, 9, 0), "staff-1", "application/pdf")
    val group = DataGenerator.generateGroups(assignedGroup, establishmentId, "Test Group", emptyList(), GroupType.DEPARTMENT)

    Mockito.`when`(appRepository.findAppsByRequestedBy(prisonerNumber)).thenReturn(listOf(includedApp, excludedApp))
    Mockito.`when`(historyRepository.findByAppIdAndEstablishmentOrderByCreatedDate(includedApp.id, establishmentId)).thenReturn(histories)
    Mockito.`when`(applicationTypeRepository.findById(typeId)).thenReturn(Optional.of(applicationType))
    Mockito.`when`(appFileRepository.findById(fileId)).thenReturn(Optional.of(appFile))
    Mockito.`when`(groupRepository.findById(assignedGroup)).thenReturn(Optional.of(group))
    Mockito.`when`(commentRepository.getCommentsByAppIdOrderByCreatedDateDesc(includedApp.id)).thenReturn(emptyList())
    Mockito.`when`(responseRepository.findByAppId(includedApp.id)).thenReturn(emptyList())

    val result = sarService.getPrisonContentFor(
      prisonerNumber,
      LocalDate.of(2026, 1, 1),
      LocalDate.of(2026, 1, 15),
    )

    assertNotNull(result)
    val sarContent = assertInstanceOf(SarContent::class.java, result!!.content)
    assertEquals(firstName, sarContent.firstName)
    assertEquals(lastName, sarContent.lastName)
    assertEquals(prisonerNumber, sarContent.prisonerId)
    assertEquals(1, sarContent.apps.size)

    val prnApp = sarContent.apps.first()
    assertEquals(includedApp.id, prnApp.id)
    assertEquals(AppStatus.PENDING, prnApp.status)
    assertEquals("Add social contact", prnApp.type)
    assertEquals(establishmentId, prnApp.establishment)
    assertEquals(includedApp.requests, prnApp.formData)
    assertEquals(
      listOf(
        "App request submitted.",
        "File proof-of-id.pdf added to app request",
        "App request approved.",
      ),
      prnApp.history.map { it.activity },
    )

    Mockito.verify(historyRepository).findByAppIdAndEstablishmentOrderByCreatedDate(includedApp.id, establishmentId)
    Mockito.verify(historyRepository, Mockito.never()).findByAppIdAndEstablishmentOrderByCreatedDate(excludedApp.id, establishmentId)
  }

  @Test
  fun `convert apps to sar content returns null for empty apps list`() {
    val result = sarService.convertAppsToSarContent(emptyList())

    assertNull(result)
  }
}
