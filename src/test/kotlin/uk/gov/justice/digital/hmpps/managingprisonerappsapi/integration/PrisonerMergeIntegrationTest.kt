package uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.HistoryRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.AdditionalInformationMerge
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.HMPPSMergeDomainEvent
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.PrisonerEventSubscriberService.Companion.PRISONER_MERGE_EVENT_TYPE
import java.time.Duration
import java.time.Instant

private const val OLD_NOMS_NUMBER = "A1234AA"
private const val NEW_NOMS_NUMBER = "B1234BB"

class PrisonerMergeIntegrationTest : SqsIntegrationTestBase() {

  @Autowired
  lateinit var appRepository: AppRepository

  @Autowired
  lateinit var commentRepository: CommentRepository

  @Autowired
  lateinit var historyRepository: HistoryRepository

  private val awaitAtMost30Secs
    get() = await.atMost(Duration.ofSeconds(30))

  @Test
  @Sql(
    "classpath:test_data/reset.sql",
    "classpath:test_data/prisoner-merge.sql",
  )
  fun `should merge prisoner app records when merge event received`() {
    // ARRANGE - Verify initial state
    assertThat(appRepository.findAppsByRequestedBy(OLD_NOMS_NUMBER)).hasSize(3)
    assertThat(appRepository.findAppsByRequestedBy(NEW_NOMS_NUMBER)).hasSize(1)

    // ACT - Publish merge event to SNS topic
    publishDomainEventMessage(
      PRISONER_MERGE_EVENT_TYPE,
      AdditionalInformationMerge(
        removedNomsNumber = OLD_NOMS_NUMBER,
        nomsNumber = NEW_NOMS_NUMBER,
      ),
      "A prisoner has been merged from $OLD_NOMS_NUMBER to $NEW_NOMS_NUMBER",
    )

    // WAIT - Wait for async processing to complete
    awaitAtMost30Secs untilAsserted {
      verify(eventProcessingComplete).complete()
    }

    // ASSERT - Verify merge happened correctly
    // Old NOMS should have 0 apps
    assertThat(appRepository.findAppsByRequestedBy(OLD_NOMS_NUMBER)).hasSize(0)
    // New NOMS should have 4 apps (3 merged + 1 existing)
    assertThat(appRepository.findAppsByRequestedBy(NEW_NOMS_NUMBER)).hasSize(4)

    // Verify history entries were created (3 for merged apps)
    val historyEntries = historyRepository.findAll()
    assertThat(historyEntries).hasSizeGreaterThanOrEqualTo(3)

    // Verify telemetry event was tracked by TelemetryService
    // The service calls addTelemetryDataForPrisonerMerge which adds dateTime dynamically
    verify(telemetryClient).trackEvent(
      eq("PRISONER_ID_UPDATE"),
      argThat { map ->
        map["requestedBy"] == NEW_NOMS_NUMBER &&
          map["appType"] == "0" &&
          map["appGroup"] == "1" &&
          map["createdBy"] == "MANAGE_APPS_ADMIN" &&
          map.containsKey("dateTime") // dateTime is added by TelemetryService
      },
      isNull(),
    )

    // Verify queue is empty (all messages processed)
    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(0)
  }

  @Test
  @Sql(
    "classpath:test_data/reset.sql",
    "classpath:test_data/prisoner-merge.sql",
  )
  fun `should not track telemetry event when no apps to merge`() {
    val nonExistentNomsNumber = "ZZ9999ZZ"

    // Verify no apps exist for this NOMS number
    assertThat(appRepository.findAppsByRequestedBy(nonExistentNomsNumber)).hasSize(0)

    // Publish merge event
    publishDomainEventMessage(
      PRISONER_MERGE_EVENT_TYPE,
      AdditionalInformationMerge(
        removedNomsNumber = nonExistentNomsNumber,
        nomsNumber = NEW_NOMS_NUMBER,
      ),
      "A prisoner has been merged from $nonExistentNomsNumber to $NEW_NOMS_NUMBER",
    )

    // Wait for processing to complete
    awaitAtMost30Secs untilAsserted {
      verify(eventProcessingComplete).complete()
    }

    // Verify no telemetry event was tracked (because no apps were merged)
    verifyNoInteractions(telemetryClient)

    // Verify queue is empty
    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(0)
  }

  private fun publishDomainEventMessage(
    eventType: String,
    additionalInformation: AdditionalInformationMerge,
    description: String,
  ) {
    val domainEvent = HMPPSMergeDomainEvent(
      eventType = eventType,
      additionalInformation = additionalInformation,
      occurredAt = Instant.now().toString(),
      description = description,
      version = "1.0",
    )

    val messageJson = jsonString(domainEvent)

    // Publish to SNS topic - using exact same pattern as DevTestResource
    domainEventsTopic.snsClient.publish {
      it.topicArn(domainEventsTopic.arn)
        .message(messageJson)
        .messageAttributes(
          mapOf(
            "eventType" to software.amazon.awssdk.services.sns.model.MessageAttributeValue.builder()
              .dataType("String")
              .stringValue(eventType)
              .build(),
          ),
        )
    }
  }
}
