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
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.CommentVisibility
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Decision
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Groups
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.History
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Response
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppFileRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationTypeRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.HistoryRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ResponseRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SubjectAccessRequestResponse
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

  override fun getFromDate(): LocalDate = LocalDate.of(2025, 11, 1)

  override fun getToDate(): LocalDate = LocalDate.of(2026, 3, 31)

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
  fun `SAR report should render as HTML as expected`() {
    setupTestData()

    getSarHelper().stubFindPrisonNameWith("Moorland (HMP & YOI)")
    getSarHelper().stubFindUserLastNameWith("Johnson")

    val dataResponse = requestSarData(
      getPrn(),
      getCrn(),
      getFromDate(),
      getToDate(),
    )

    // Extract attachment URLs from the SAR response and stub each with the real image bytes
    extractAttachmentUrls(dataResponse).forEach { url ->
      getSarHelper().stubGetAttachment(url, fetchImageBytes(url))
    }

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

  // TODO: Uncomment when hmpps-subject-access-request-test-support library is upgraded with PDF support
  @Test
  fun `SAR report should render as PDF as expected`() {
    setupTestData()

    getSarHelper().stubFindPrisonNameWith("Moorland (HMP & YOI)")
    getSarHelper().stubFindUserLastNameWith("Johnson")

    val dataResponse = requestSarData(
      getPrn(),
      getCrn(),
      getFromDate(),
      getToDate(),
    )

    // Extract attachment URLs from the SAR response and stub each with the real image bytes
    extractAttachmentUrls(dataResponse).forEach { url ->
      getSarHelper().stubGetAttachment(url, fetchImageBytes(url))
    }

    val templateResponse = requestSarTemplate()

    // Render HTML first, then convert to PDF
    val htmlResult = getSarHelper().renderServiceReport(
      data = dataResponse,
      templateVersion = "1.0",
      template = templateResponse,
    )

    // renderAndSaveReportAsPdf renders HTML → PDF and saves to build/test-generated/sar-generated-report.pdf
    getSarHelper().renderAndSaveReportAsPdf(
      html = htmlResult,
      prn = getPrn(),
      crn = getCrn(),
    )
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

    // App 1: Make a general PIN phone enquiry (PIN_PHONE_ADD_NEW_SOCIAL_CONTACT)
    applicationTypeRepository.save(ApplicationType(APPLICATION_TYPE_ID_1, "Make a general PIN phone enquiry", false, false, false))
    // App 2: Add emergency phone credit (PIN_PHONE_EMERGENCY_CREDIT_TOP_UP)
    applicationTypeRepository.save(ApplicationType(APPLICATION_TYPE_ID_2, "Add emergency phone credit", false, false, false))

    groupRepository.save(
      Groups(
        id = BUSINESS_HUB_GROUP_ID,
        name = "Business Hub",
        establishmentId = ESTABLISHMENT_ID,
        initialsApps = emptyList(),
        initialsApplicationTypes = emptyList(),
        type = GroupType.DEPARTMENT,
      ),
    )

    groupRepository.save(
      Groups(
        id = OMU_GROUP_ID,
        name = "OMU",
        establishmentId = ESTABLISHMENT_ID,
        initialsApps = emptyList(),
        initialsApplicationTypes = emptyList(),
        type = GroupType.DEPARTMENT,
      ),
    )

    // App 1: PIN phone enquiry with attachments
    val app1 = App(
      id = APP_ID_1,
      reference = "019cd286-b576-7357-9e6c-b221b7d3e7d6",
      assignedGroup = BUSINESS_HUB_GROUP_ID,
      appType = AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT,
      applicationGroup = 1,
      applicationType = APPLICATION_TYPE_ID_1,
      genericForm = false,
      requestedDate = LocalDateTime.of(2026, 3, 9, 12, 16, 9),
      createdDate = LocalDateTime.of(2026, 3, 9, 12, 16, 9),
      submittedByType = UserCategory.STAFF,
      createdBy = "CHIEMNDEV",
      lastModifiedDate = LocalDateTime.of(2026, 3, 9, 12, 16, 9),
      lastModifiedBy = "CHIEMNDEV",
      comments = mutableListOf(COMMENT_ID_1),
      requests = listOf(
        linkedMapOf<String, Any>(
          "id" to "5a94f455-956c-493d-8071-983c831716bf",
          "lastName" to "Patts",
          "firstName" to "John",
          "telephone1" to "441234567899",
          "telephone2" to "",
          "relationship" to "Brother",
        ),
      ),
      requestedBy = PRISONER_NUMBER,
      requestedByFirstName = "OBLAMONAIN",
      requestedByLastName = "AALYLE",
      status = AppStatus.PENDING,
      establishmentId = ESTABLISHMENT_ID,
      responses = mutableListOf(RESPONSE_ID_1),
      firstNightCenter = false,
      appFiles = mutableListOf(),
    )

    appRepository.save(app1)

    // App 2: Emergency phone credit without attachments
    val app2 = App(
      id = APP_ID_2,
      reference = "019a6e8d-12c9-785c-8d0c-b84cd97398ec",
      assignedGroup = OMU_GROUP_ID,
      appType = AppType.PIN_PHONE_EMERGENCY_CREDIT_TOP_UP,
      applicationGroup = 1,
      applicationType = APPLICATION_TYPE_ID_2,
      genericForm = false,
      requestedDate = LocalDateTime.of(2025, 11, 10, 16, 15, 30),
      createdDate = LocalDateTime.of(2025, 11, 10, 16, 15, 30),
      submittedByType = UserCategory.STAFF,
      createdBy = "SMJOHN_GEN",
      lastModifiedDate = LocalDateTime.of(2025, 12, 8, 12, 36, 34),
      lastModifiedBy = "SMJOHN_GEN",
      comments = mutableListOf(COMMENT_ID_2),
      requests = listOf(
        linkedMapOf<String, Any>(
          "id" to "998e4959-61ec-449d-908b-f219aced9e0d",
          "amount" to "1",
          "reason" to "testing",
        ),
      ),
      requestedBy = PRISONER_NUMBER,
      requestedByFirstName = "OBLAMONAIN",
      requestedByLastName = "AALYLE",
      status = AppStatus.PENDING,
      establishmentId = ESTABLISHMENT_ID,
      responses = mutableListOf(RESPONSE_ID_2),
      firstNightCenter = false,
      appFiles = mutableListOf(),
    )

    appRepository.save(app2)

    commentRepository.saveAll(
      listOf(
        Comment(
          id = COMMENT_ID_1,
          message = "Need some more info",
          createdDate = LocalDateTime.of(2026, 2, 27, 11, 52, 15),
          createdBy = "SMJOHN_GEN",
          appId = APP_ID_1,
          visibility = CommentVisibility.STAFF_AND_PRISONER,
          createdByUserType = UserCategory.STAFF,
        ),
        Comment(
          id = COMMENT_ID_2,
          message = "",
          createdDate = LocalDateTime.of(2025, 12, 8, 12, 36, 34),
          createdBy = "SMJOHN_GEN",
          appId = APP_ID_2,
          visibility = CommentVisibility.STAFF_AND_PRISONER,
          createdByUserType = UserCategory.STAFF,
        ),
      ),
    )

    responseRepository.saveAll(
      listOf(
        Response(
          id = RESPONSE_ID_1,
          reason = "",
          decision = Decision.APPROVED,
          createdDate = LocalDateTime.of(2026, 2, 27, 11, 52, 15),
          createdBy = "SMJOHN_GEN",
          app1.id,
        ),
        Response(
          id = RESPONSE_ID_2,
          reason = "test reason",
          decision = Decision.DECLINED,
          createdDate = LocalDateTime.of(2025, 12, 8, 12, 36, 34),
          createdBy = "JADAMS_GEN",
          app1.id,
        ),
      ),
    )

    // App 1 has two attachments
    appFileRepository.saveAll(
      listOf(
        AppFile(
          id = FILE_ID_1,
          documentId = "b4de95fd-903b-4df3-8430-e99e5b4cd063",
          fileName = "attachment1.png",
          createdDate = LocalDateTime.of(2026, 3, 9, 12, 16, 9),
          createdBy = "CHIEMNDEV",
          fileType = "image/png",
          app = app1,
        ),
        AppFile(
          id = FILE_ID_2,
          documentId = "709b190e-4a06-4041-a799-bd6c5848212c",
          fileName = "attachment2.png",
          createdDate = LocalDateTime.of(2026, 3, 9, 12, 16, 9),
          createdBy = "CHIEMNDEV",
          fileType = "image/png",
          app = app1,
        ),
      ),
    )

    historyRepository.saveAll(
      listOf(
        // App 1 history
        History(
          id = UUID.fromString("30000000-0000-0000-0000-000000000001"),
          entityId = APP_ID_1,
          entityType = EntityType.APP,
          appId = APP_ID_1,
          activity = Activity.APP_SUBMITTED,
          establishment = ESTABLISHMENT_ID,
          createdBy = "CHIEMNDEV",
          createdDate = LocalDateTime.of(2026, 3, 9, 12, 16, 9),
          reference = null,
        ),
        // App 2 history
        History(
          id = UUID.fromString("30000000-0000-0000-0000-000000000002"),
          entityId = APP_ID_2,
          entityType = EntityType.APP,
          appId = APP_ID_2,
          activity = Activity.APP_SUBMITTED,
          establishment = ESTABLISHMENT_ID,
          createdBy = "SMJOHN_GEN",
          createdDate = LocalDateTime.of(2025, 11, 10, 16, 15, 30),
          reference = null,
        ),
        History(
          id = UUID.fromString("30000000-0000-0000-0000-000000000003"),
          entityId = APP_ID_2,
          entityType = EntityType.APP,
          appId = APP_ID_2,
          activity = Activity.APP_FORWARDED_TO_A_GROUP,
          establishment = ESTABLISHMENT_ID,
          createdBy = "SMJOHN_GEN",
          createdDate = LocalDateTime.of(2025, 12, 8, 12, 36, 34),
          reference = null,
        ),
        History(
          id = UUID.fromString("30000000-0000-0000-0000-000000000004"),
          entityId = APP_ID_2,
          entityType = EntityType.APP,
          appId = APP_ID_2,
          activity = Activity.FORWARDING_COMMENT_ADDED,
          establishment = ESTABLISHMENT_ID,
          createdBy = "SMJOHN_GEN",
          createdDate = LocalDateTime.of(2025, 12, 8, 12, 36, 34),
          reference = null,
        ),
      ),
    )
  }

  private companion object {
    private const val PRISONER_NUMBER = "G5829VO"
    private const val ESTABLISHMENT_ID = "PNI"
    private const val APPLICATION_TYPE_ID_1 = 1L
    private const val APPLICATION_TYPE_ID_2 = 2L
    private val APP_ID_1: UUID = UUID.fromString("019cd286-b576-7357-9e6c-b221b7d3e7d6")
    private val APP_ID_2: UUID = UUID.fromString("019a6e8d-12c9-785c-8d0c-b84cd97398ec")
    private val FILE_ID_1: UUID = UUID.fromString("20000000-0000-0000-0000-000000000001")
    private val FILE_ID_2: UUID = UUID.fromString("20000000-0000-0000-0000-000000000002")
    private val BUSINESS_HUB_GROUP_ID: UUID = UUID.fromString("40000000-0000-0000-0000-000000000001")
    private val OMU_GROUP_ID: UUID = UUID.fromString("40000000-0000-0000-0000-000000000002")
    private val COMMENT_ID_1: UUID = UUID.fromString("50000000-0000-0000-0000-000000000001")
    private val COMMENT_ID_2: UUID = UUID.fromString("50000000-0000-0000-0000-000000000002")
    private val RESPONSE_ID_1: UUID = UUID.fromString("60000000-0000-0000-0000-000000000001")
    private val RESPONSE_ID_2: UUID = UUID.fromString("60000000-0000-0000-0000-000000000002")

    // URLs match what SarServiceImpl builds per AppFile. Currently using a dummy URL for all attachments.
    // When real document API is used, update to: "\${documentApiUrl}/documents/{documentId}/file"

    // Fetch actual image bytes from the URL so the real image appears in generated HTML/PDF.
    // Falls back to the PNG test resource if the URL is not reachable (e.g. in CI with no internet).
    private fun fetchImageBytes(url: String): ByteArray = try {
      java.net.URI(url).toURL().openStream().use { it.readBytes() }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      SarIntegrationTest::class.java
        .getResourceAsStream("/sar/attachments/test-attachment.png")!!
        .readBytes()
    }
  }

  /**
   * Extracts all attachment URLs from the SAR response content by parsing the JSON.
   * This avoids hardcoding URLs in the test - the URLs are read directly from what
   * SarServiceImpl returns, so they always stay in sync.
   */
  private fun extractAttachmentUrls(response: SubjectAccessRequestResponse): List<String> {
    val json = getSarHelper().toJson(response)
    val root = com.fasterxml.jackson.databind.ObjectMapper().readTree(json)
    return root.path("apps").flatMap { app ->
      app.path("appAttachments").map { attachment -> attachment.path("url").asText() }
    }.filter { it.isNotBlank() }
  }
}
