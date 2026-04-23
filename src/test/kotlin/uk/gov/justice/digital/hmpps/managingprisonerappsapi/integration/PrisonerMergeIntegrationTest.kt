package uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.HistoryRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.events.AdditionalInformationMerge
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.events.HMPPSMergeDomainEvent
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.events.PrisonerEventSubscriberService.Companion.PRISONER_MERGE_EVENT_TYPE
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator.Companion.assignedGroup
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator.Companion.generateAppForMerge
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

private const val OLD_NOMS_NUMBER = "A1234AA"
private const val NEW_NOMS_NUMBER = "B1234BB"

class PrisonerMergeIntegrationTest : SqsIntegrationTestBase() {

  @Autowired
  lateinit var appRepository: AppRepository

  @Autowired
  lateinit var historyRepository: HistoryRepository

  private val awaitAtMost30Secs
    get() = await.atMost(Duration.ofSeconds(30))

  @BeforeEach
  fun setUp() {
    // Clean up existing data
    historyRepository.deleteAll()
    appRepository.deleteAll()

    // Create 3 apps for old NOMS number
    appRepository.save(generateOldNomsMergeApp1())
    appRepository.save(generateOldNomsMergeApp2())
    appRepository.save(generateOldNomsMergeApp3())

    // Create 1 app for new NOMS number (already exists)
    appRepository.save(generateNewNomsMergeApp())
  }

  @Test
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
        map["newPrisoneId"] == NEW_NOMS_NUMBER &&
          map["removedPrisoneId"] == OLD_NOMS_NUMBER &&
          map["createdBy"] == "MANAGE_APPS_ADMIN" &&
          map.containsKey("dateTime") // dateTime is added by TelemetryService
      },
      isNull(),
    )

    // Verify queue is empty (all messages processed)
    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(0)
  }

  @Test
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
