package uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.helpers.LocalStackContainer
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.helpers.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.EventProcessingComplete
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.autoconfigure.exclude="])
abstract class SqsIntegrationTestBase : IntegrationTestBase() {

  @Autowired
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  @MockitoSpyBean
  protected lateinit var telemetryClient: TelemetryClient

  @MockitoBean
  lateinit var eventProcessingComplete: EventProcessingComplete

  protected val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw MissingQueueException("HmppsTopic domainevents not found")
  }
  protected val domainEventsTopicSnsClient by lazy { domainEventsTopic.snsClient }
  protected val domainEventsTopicArn by lazy { domainEventsTopic.arn }

  protected val domainEventsQueue by lazy { 
    hmppsQueueService.findByQueueId("domaineventsqueue") as HmppsQueue 
  }

  @BeforeEach
  fun cleanQueue() {
    domainEventsQueue.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(domainEventsQueue.queueUrl).build(),
    )
    await untilCallTo {
      domainEventsQueue.sqsClient.countMessagesOnQueue(domainEventsQueue.queueUrl).get()
    } matches { it == 0 }
  }

  fun getNumberOfMessagesCurrentlyOnQueue(): Int? = 
    domainEventsQueue.sqsClient.countMessagesOnQueue(domainEventsQueue.queueUrl).get()

  protected fun jsonString(any: Any): String = objectMapper.writeValueAsString(any)

  companion object {
    private val localStackContainer = LocalStackContainer.instance

    @Suppress("unused")
    @JvmStatic
    @DynamicPropertySource
    fun testcontainers(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }
}

