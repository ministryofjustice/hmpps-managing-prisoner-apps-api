package uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.AfterEach
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.config.SarTestConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SubjectAccessRequestResponse
import java.nio.file.Paths
import java.time.LocalDate
import java.util.Optional

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
    )
  }

  @Autowired
  protected lateinit var sarTestConfig: SarTestConfig

  /**
   * When generateActual is true, copies the .log scratch files written by the library
   * into src/test/resources/sar/expected/ so they become the new expected snapshots.
   *
   * File mappings:
   *   entity-schema.json.log          -> sar/expected/jpa-entity-schema.json
   *   sar-api-response.json.log       -> sar/expected/sar-api-response.json
   *   sar-generated-report.html.log   -> sar/expected/sar-report-output.html
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
  )

  abstract fun getSarHelper(): SarIntegrationTestHelper

  abstract fun setupTestData()

  open fun getPrn(): String? = null

  open fun getCrn(): String? = null

  open fun getFromDate(): LocalDate? = null

  open fun getToDate(): LocalDate? = null

  abstract fun getEntityManagerInstance(): EntityManager

  /**
   * Workaround for Spring 6.2 incompatibility: StatusAssertions.isOk() now returns void
   * instead of WebTestClient.ResponseSpec, breaking the library's chaining.
   * Uses isEqualTo(HttpStatus.OK) which still returns ResponseSpec.
   */
  protected fun requestSarData(
    prn: String?,
    crn: String?,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): SubjectAccessRequestResponse {
    val objectMapper = JsonMapper.builder()
      .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
      .build()

    val response = webTestClient.get().uri {
      it.path("/subject-access-request")
        .queryParamIfPresent("prn", Optional.ofNullable(prn))
        .queryParamIfPresent("crn", Optional.ofNullable(crn))
        .queryParamIfPresent("fromDate", Optional.ofNullable(fromDate))
        .queryParamIfPresent("toDate", Optional.ofNullable(toDate))
        .build()
    }
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.OK)
      .expectBody(String::class.java)
      .returnResult().responseBody!!

    return objectMapper.readValue(response, SubjectAccessRequestResponse::class.java)
  }

  /**
   * Workaround for Spring 6.2 incompatibility for the template endpoint.
   */
  protected fun requestSarTemplate(): String = webTestClient
    .get().uri {
      it.path("/subject-access-request/template").build()
    }
    .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
    .exchange()
    .expectStatus().isEqualTo(HttpStatus.OK)
    .expectBody(String::class.java)
    .returnResult().responseBody!!
}
