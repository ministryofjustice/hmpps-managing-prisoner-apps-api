package uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.AfterEach
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.config.SarTestConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SubjectAccessRequestResponse
import java.nio.file.Paths
import java.time.LocalDate

/**
 * Base class for SAR integration tests that extends IntegrationTestBase
 * and provides SAR-specific test functionality using the framework's SarIntegrationTestHelper.
 *
 * Concrete subclasses must implement:
 *  - setupTestData()  — insert the test fixtures needed for this scenario
 *  - getSarHelper()   — return the configured SarIntegrationTestHelper
 *  - getEntityManagerInstance() — return the injected EntityManager
 *
 * The three SAR tests (JPA schema, API response, HTML render) live in the subclass so
 * each subclass owns its full test lifecycle.
 */
@Import(SarIntegrationTestHelperConfig::class)
abstract class SarIntegrationTestBase : IntegrationTestBase() {

  companion object {
    private val log = LoggerFactory.getLogger(SarIntegrationTestBase::class.java)

    private val LOG_TO_EXPECTED = mapOf(
      "entity-schema.json.log" to "jpa-entity-schema.json",
      "sar-api-response.json.log" to "sar-api-response.json",
      "sar-generated-report.html.log" to "sar-report-output.html",
      "sar-generated-report.pdf.log" to "sar-report-output.pdf",
    )
  }

  @Autowired
  protected lateinit var sarTestConfig: SarTestConfig

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  /**
   * When generateActual is true, copies the .log scratch files written by the library
   * into src/test/resources/sar/expected/ so they become the new expected snapshots.
   */
  @AfterEach
  fun tearDown() {
    if (!sarTestConfig.generateActual) return

    val testResourcesDir = Paths.get(System.getProperty("user.dir"), "src", "test", "resources").toFile()
    val expectedDir = testResourcesDir.resolve("sar/expected").also { it.mkdirs() }

    LOG_TO_EXPECTED.forEach { (logFileName, expectedFileName) ->
      val src = testResourcesDir.resolve(logFileName)
      val dst = expectedDir.resolve(expectedFileName)
      if (src.exists()) {
        src.copyTo(dst, overwrite = true)
        log.info("Promoted snapshot: $logFileName -> sar/expected/$expectedFileName")
      }
    }
  }

  /**
   * Creates SarIntegrationTestHelper with configuration from application-test.yml
   */
  protected fun createSarIntegrationTestHelper(): SarIntegrationTestHelper = SarIntegrationTestHelper(
    jwtAuthHelper = jwtAuthHelper,
    expectedApiResponsePath = sarTestConfig.expectedApiResponsePath,
    expectedRenderResultPath = sarTestConfig.expectedRenderResultPath,
    attachmentsExpected = sarTestConfig.attachmentsExpected,
    expectedFlywaySchemaVersion = sarTestConfig.expectedFlywaySchemaVersion,
    expectedJpaEntitySchemaPath = sarTestConfig.expectedJpaEntitySchemaPath,
    objectMapper = objectMapper,
  )

  abstract fun getSarHelper(): SarIntegrationTestHelper

  abstract fun setupTestData()

  open fun getPrn(): String? = null

  open fun getCrn(): String? = null

  open fun getFromDate(): LocalDate? = null

  open fun getToDate(): LocalDate? = null

  abstract fun getEntityManagerInstance(): EntityManager

  /**
   * Delegates to the library's requestSarData which calls GET /subject-access-request
   * and queries the actual database with the test data inserted by setupTestData().
   */
  protected fun requestSarData(
    prn: String?,
    crn: String?,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): SubjectAccessRequestResponse<Any> = getSarHelper().requestSarData(prn, crn, fromDate, toDate, webTestClient, Any::class.java)

  /**
   * Delegates to the library's requestSarTemplate which calls GET /subject-access-request/template
   * and returns the actual sar_template.mustache from src/main/resources.
   */
  protected fun requestSarTemplate(): String = getSarHelper().requestSarTemplate(webTestClient)
}
