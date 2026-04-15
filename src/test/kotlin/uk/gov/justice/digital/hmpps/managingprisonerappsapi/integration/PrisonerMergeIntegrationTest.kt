package uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.HistoryRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.AdditionalInformationMerge
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.HMPPSEventType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.HMPPSMessage
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.HMPPSMergeDomainEvent
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.HMPPSMessageAttributes
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.MERGE_EVENT_NAME
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

    // ACT - Publish merge event to SQS queue
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

    // Verify comments were created for merged apps
    val mergedApps = appRepository.findAppsByRequestedBy(NEW_NOMS_NUMBER)
    
    // Count apps with merge comments by checking the comment repository
    var appsWithMergeCommentsCount = 0
    mergedApps.forEach { app ->
      val comments = commentRepository.findAll().filter { it.appId == app.id }
      if (comments.any { comment ->
        comment.message.contains("Prisoner merged from $OLD_NOMS_NUMBER to $NEW_NOMS_NUMBER")
      }) {
        appsWithMergeCommentsCount++
      }
    }
    assertThat(appsWithMergeCommentsCount).isEqualTo(3) // 3 merged apps should have new comments

    // Verify history entries were created
    val historyEntries = historyRepository.findAll()
    assertThat(historyEntries).hasSizeGreaterThanOrEqualTo(3)

    // Verify telemetry event was tracked
    verify(telemetryClient).trackEvent(
      MERGE_EVENT_NAME,
      mapOf(
        "MERGE-FROM" to OLD_NOMS_NUMBER,
        "MERGE-TO" to NEW_NOMS_NUMBER,
        "APPS-MERGED" to "3",
      ),
      null,
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
    
    // Wrap the domain event in the HMPPSMessage structure that the listener expects
    val hmppsMessage = HMPPSMessage(
      message = jsonString(domainEvent),
      messageAttributes = HMPPSMessageAttributes(
        eventType = HMPPSEventType(
          value = eventType,
          type = "String"
        )
      )
    )
    
    // Send directly to SQS queue
    domainEventsQueue.sqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(domainEventsQueue.queueUrl)
        .messageBody(jsonString(hmppsMessage))
        .build()
    ).get()
  }
}
