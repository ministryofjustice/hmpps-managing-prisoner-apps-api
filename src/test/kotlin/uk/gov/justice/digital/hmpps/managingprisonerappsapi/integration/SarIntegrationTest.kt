package uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppFile
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Decision
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Groups
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.History
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Response
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppFileRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationTypeRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.HistoryRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ResponseRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SarIntegrationTest : SarIntegrationTestBase() {

  @Autowired
  private lateinit var appRepository: AppRepository

  @Autowired
  private lateinit var applicationTypeRepository: ApplicationTypeRepository

  @Autowired
  private lateinit var historyRepository: HistoryRepository

  @Autowired
  private lateinit var appFileRepository: AppFileRepository

  @Autowired
  private lateinit var groupRepository: GroupRepository

  @Autowired
  private lateinit var commentRepository: CommentRepository

  @Autowired
  private lateinit var responseRepository: ResponseRepository

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val sarIntegrationHelper: SarIntegrationTestHelper by lazy {
    createSarIntegrationTestHelper()
  }

  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationHelper

  override fun getEntityManagerInstance(): EntityManager = entityManager

  override fun getPrn(): String = PRISONER_NUMBER

  override fun getFromDate(): LocalDate = LocalDate.of(2026, 1, 1)

  override fun getToDate(): LocalDate = LocalDate.of(2026, 1, 16)

  @Test
  fun `JPA generated entity schema should match expected snapshot`() {
    val currentSchema = getSarHelper().getGeneratedEntitySchema(getEntityManagerInstance())
    if (sarTestConfig.generateActual) {
      getSarHelper().saveEntitySchema(currentSchema)
    } else {
      assertThatJson(currentSchema).`as`("JPA entity schema")
        .isEqualTo(getSarHelper().getExpectedSchemaSnapshot())
    }
  }

  @Test
  fun `SAR report should render as expected`() {
    setupTestData()

    getSarHelper().stubFindPrisonNameWith("Moorland (HMP & YOI)")
    getSarHelper().stubFindUserLastNameWith("Johnson")

    val dataResponse = requestSarData(
      getPrn(),
      getCrn(),
      getFromDate(),
      getToDate(),
    )
    val templateResponse = requestSarTemplate()

    val renderResult = getSarHelper().renderServiceReport(
      data = dataResponse,
      templateVersion = "1.0",
      template = templateResponse,
    )

    if (sarTestConfig.generateActual) {
      getSarHelper().saveGeneratedReport(renderResult)
    } else {
      getSarHelper().assertHtmlEquals(renderResult, getSarHelper().getExpectedRenderResult())
    }
  }

  @Test
  fun `SAR endpoint should return expected data`() {
    setupTestData()

    val response = requestSarData(
      getPrn(),
      getCrn(),
      getFromDate(),
      getToDate(),
    )

    if (sarTestConfig.generateActual) {
      getSarHelper().saveSarApiResponse(response)
    } else {
      assertThatJson(getSarHelper().toJson(response)).`as`("Response content json")
        .isEqualTo(getSarHelper().getExpectedSarJson())
      assertThat(response.attachments?.isNotEmpty() == true).`as`("Response has attachments")
        .isEqualTo(getSarHelper().attachmentsExpected)
    }
  }

  @Test
  fun `SAR endpoint should return 401 when called without a token`() {
    webTestClient.get().uri {
      it.path("/subject-access-request")
        .queryParam("prn", PRISONER_NUMBER)
        .build()
    }
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `SAR endpoint should return 403 when called with a token that does not have the ROLE_SAR_DATA_ACCESS role`() {
    webTestClient.get().uri {
      it.path("/subject-access-request")
        .queryParam("prn", PRISONER_NUMBER)
        .build()
    }
      .headers(setAuthorisation(roles = listOf("ROLE_PRISON")))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `SAR endpoint should return 209 and empty response body when the SAR endpoint param is CRN`() {
    webTestClient.get().uri {
      it.path("/subject-access-request")
        .queryParam("crn", "X12345")
        .build()
    }
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isEqualTo(209)
      .expectBody().isEmpty
  }

  @Test
  fun `SAR endpoint respond should return 204 and empty response when the SAR endpoint has valid token and prisoner has no data`() {
    webTestClient.get().uri {
      it.path("/subject-access-request")
        .queryParam("prn", "A99999")
        .build()
    }
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isNoContent
      .expectBody().isEmpty
  }

  override fun setupTestData() {
    historyRepository.deleteAll()
    appFileRepository.deleteAll()
    appRepository.deleteAll()
    applicationTypeRepository.deleteAll()
    groupRepository.deleteAll()
    commentRepository.deleteAll()
    responseRepository.deleteAll()

    applicationTypeRepository.save(ApplicationType(APPLICATION_TYPE_ID, "Add emergency phone credit", false, false, false))

    groupRepository.save(
      Groups(
        id = ASSIGNED_GROUP_ID,
        name = "Reception Group",
        establishmentId = ESTABLISHMENT_ID,
        initialsApps = emptyList(),
        initialsApplicationTypes = emptyList(),
        type = GroupType.DEPARTMENT,
      ),
    )

    val app1 = App(
      id = APP_ID,
      reference = "SAR-REF-1",
      assignedGroup = ASSIGNED_GROUP_ID,
      appType = AppType.PIN_PHONE_EMERGENCY_CREDIT_TOP_UP,
      applicationGroup = 1,
      applicationType = APPLICATION_TYPE_ID,
      genericForm = false,
      requestedDate = LocalDateTime.of(2026, 1, 10, 10, 30),
      createdDate = LocalDateTime.of(2026, 1, 10, 10, 30),
      createdBy = "staff.one",
      lastModifiedDate = LocalDateTime.of(2026, 1, 15, 0, 0),
      lastModifiedBy = "staff.two",
      comments = mutableListOf(COMMENT_ID_1, COMMENT_ID_2),
      requests = listOf(
        linkedMapOf<String, Any>(
          "amount" to "478.50",
          "reason" to "Some sort of emergency",
          "contactNumber" to "07123456789",
          "firstName" to "Alex",
          "lastName" to "Smith",
        ),
      ),
      requestedBy = PRISONER_NUMBER,
      requestedByFirstName = "Jane",
      requestedByLastName = "Doe",
      status = AppStatus.PENDING,
      establishmentId = ESTABLISHMENT_ID,
      responses = mutableListOf(RESPONSE_ID),
      firstNightCenter = false,
      appFiles = mutableListOf(),
    )

    appRepository.save(app1)

    commentRepository.saveAll(
      listOf(
        Comment(
          id = COMMENT_ID_1,
          message = "This is the first comment on the application",
          createdDate = LocalDateTime.of(2026, 1, 11, 14, 30),
          createdBy = "staff.one",
          appId = APP_ID,
        ),
        Comment(
          id = COMMENT_ID_2,
          message = "This is a follow-up comment with additional information",
          createdDate = LocalDateTime.of(2026, 1, 12, 10, 15),
          createdBy = "staff.two",
          appId = APP_ID,
        ),
      ),
    )

    responseRepository.save(
      Response(
        id = RESPONSE_ID,
        reason = "Application meets all requirements and is approved",
        decision = Decision.APPROVED,
        createdDate = LocalDateTime.of(2026, 1, 14, 16, 45),
        createdBy = "staff.two",
      ),
    )

    appFileRepository.saveAll(
      listOf(
        AppFile(
          id = FILE_ID_1,
          documentId = "6a1300ca-3011-4fd2-a8ad-5cbd736de041",
          fileName = "application-photo[photo1].jpg",
          createdDate = LocalDateTime.of(2026, 1, 11, 9, 0),
          createdBy = "staff.one",
          fileType = "image/jpeg",
          app = app1,
        ),
        AppFile(
          id = FILE_ID_2,
          documentId = "6b1300ca-3011-4fd2-a8ad-5cbd736de065",
          fileName = "application-photo[photo2].jpg",
          createdDate = LocalDateTime.of(2025, 10, 11, 9, 0),
          createdBy = "staff.two",
          fileType = "image/jpeg",
          app = app1,
        ),
      ),
    )

    historyRepository.saveAll(
      listOf(
        History(
          id = UUID.fromString("30000000-0000-0000-0000-000000000001"),
          entityId = APP_ID,
          entityType = EntityType.APP,
          appId = APP_ID,
          activity = Activity.APP_SUBMITTED,
          establishment = ESTABLISHMENT_ID,
          createdBy = "staff.one",
          createdDate = LocalDateTime.of(2026, 1, 10, 11, 0),
        ),
        History(
          id = UUID.fromString("30000000-0000-0000-0000-000000000002"),
          entityId = FILE_ID_1,
          entityType = EntityType.FILE,
          appId = APP_ID,
          activity = Activity.FILE_ADDED,
          establishment = ESTABLISHMENT_ID,
          createdBy = "staff.one",
          createdDate = LocalDateTime.of(2026, 1, 11, 9, 15),
        ),
        History(
          id = UUID.fromString("30000000-0000-0000-0000-000000000003"),
          entityId = APP_ID,
          entityType = EntityType.APP,
          appId = APP_ID,
          activity = Activity.APP_APPROVED,
          establishment = ESTABLISHMENT_ID,
          createdBy = "staff.two",
          createdDate = LocalDateTime.of(2026, 1, 15, 0, 0),
        ),
      ),
    )
  }

  private companion object {
    private const val PRISONER_NUMBER = "A1234BC"
    private const val ESTABLISHMENT_ID = "MDI"
    private const val APPLICATION_TYPE_ID = 1L
    private val APP_ID: UUID = UUID.fromString("10000000-0000-0000-0000-000000000001")
    private val FILE_ID_1: UUID = UUID.fromString("20000000-0000-0000-0000-000000000001")
    private val FILE_ID_2: UUID = UUID.fromString("24000000-0000-0000-0000-000000000001")
    private val ASSIGNED_GROUP_ID: UUID = UUID.fromString("40000000-0000-0000-0000-000000000001")
    private val COMMENT_ID_1: UUID = UUID.fromString("50000000-0000-0000-0000-000000000001")
    private val COMMENT_ID_2: UUID = UUID.fromString("50000000-0000-0000-0000-000000000002")
    private val RESPONSE_ID: UUID = UUID.fromString("60000000-0000-0000-0000-000000000001")
  }
}
