package uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.AfterEach
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
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ResponseRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.events.AdditionalInformation
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.events.HMPPSReleaseDomainEvent
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.events.PrisonerEventSubscriberService.Companion.PRISONER_RELEASE_EVENT_TYPE
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.events.PrisonerEventSubscriberService.Companion.RELEASE_REASON_RELEASED
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.events.PrisonerEventSubscriberService.Companion.RELEASE_REASON_TRANSFERRED
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.events.PrisonerEventSubscriberService.Companion.RELEASE_REASON_UNKNOWN
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator.Companion.assignedGroup
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator.Companion.generateAppForMerge
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

private const val PRISONER_NOMS_NUMBER = "C1234CC"
private const val PRISON_ID = "MDI"

class PrisonerReleaseIntegrationTest : SqsIntegrationTestBase() {
  @Autowired
  lateinit var appRepository: AppRepository

  @Autowired
  lateinit var historyRepository: HistoryRepository

  @Autowired
  lateinit var responseRepository: ResponseRepository

  private val awaitAtMost30Secs
    get() = await.atMost(Duration.ofSeconds(30))

  @BeforeEach
  fun setUp() {
    historyRepository.deleteAll()
    responseRepository.deleteAll()
    appRepository.deleteAll()
    purgeQueueSafely()
  }

  @AfterEach
  fun tearDown() {
    purgeQueueSafely()
  }

  @Test
  fun `should reject open prisoner apps when release event received with RELEASED reason`() {
    appRepository.save(generateOpenApp1())
    appRepository.save(generateOpenApp2())
    appRepository.save(generateClosedApp())

    assertThat(appRepository.findAppsByRequestedBy(PRISONER_NOMS_NUMBER)).hasSize(3)
    publishReleaseEvent(RELEASE_REASON_RELEASED, "A prisoner has been released from $PRISON_ID")

    awaitAtMost30Secs untilAsserted {
      verify(eventProcessingComplete).complete()
    }

    val apps = appRepository.findAppsByRequestedBy(PRISONER_NOMS_NUMBER)
    assertThat(apps).hasSize(3)

    // The 2 open apps should now be REJECTED
    val rejectedApps = apps.filter { it.status == AppStatus.REJECTED }
    assertThat(rejectedApps).hasSize(2)

    // The already-closed (APPROVED) app should be unchanged
    val approvedApps = apps.filter { it.status == AppStatus.APPROVED }
    assertThat(approvedApps).hasSize(1)

    // A Response and History entry should be created for each rejected app
    assertThat(responseRepository.findAll()).hasSize(2)
    assertThat(historyRepository.findAll()).hasSizeGreaterThanOrEqualTo(2)

    verify(telemetryClient).trackEvent(
      eq("PRISONER_RELEASED"),
      argThat { map ->
        map["newPrisonerId"] == PRISONER_NOMS_NUMBER &&
          map["releaseReason"] == RELEASE_REASON_RELEASED &&
          map["createdBy"] == "MANAGE_APPS_ADMIN" &&
          map["status"] == "SUCCESS" &&
          map.containsKey("dateTime")
      },
      isNull(),
    )

    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(0)
  }

  @Test
  fun `should reject open prisoner apps when release event received with TRANSFERRED reason`() {
    appRepository.save(generateOpenApp1())
    appRepository.save(generateOpenApp2())

    assertThat(appRepository.findAppsByRequestedBy(PRISONER_NOMS_NUMBER)).hasSize(2)
    publishReleaseEvent(RELEASE_REASON_TRANSFERRED, "A prisoner has been transferred from $PRISON_ID")
    awaitAtMost30Secs untilAsserted {
      verify(eventProcessingComplete).complete()
    }
    val apps = appRepository.findAppsByRequestedBy(PRISONER_NOMS_NUMBER)
    assertThat(apps.filter { it.status == AppStatus.REJECTED }).hasSize(2)
    assertThat(responseRepository.findAll()).hasSize(2)
    assertThat(historyRepository.findAll()).hasSizeGreaterThanOrEqualTo(2)
    verify(telemetryClient).trackEvent(
      eq("PRISONER_RELEASED"),
      argThat { map ->
        map["newPrisonerId"] == PRISONER_NOMS_NUMBER &&
          map["releaseReason"] == RELEASE_REASON_TRANSFERRED &&
          map["createdBy"] == "MANAGE_APPS_ADMIN" &&
          map["status"] == "SUCCESS" &&
          map.containsKey("dateTime")
      },
      isNull(),
    )
    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(0)
  }

  @Test
  fun `should reject open prisoner apps when release event received with UNKNOWN reason`() {
    appRepository.save(generateOpenApp1())
    appRepository.save(generateOpenApp2())

    assertThat(appRepository.findAppsByRequestedBy(PRISONER_NOMS_NUMBER)).hasSize(2)
    publishReleaseEvent(RELEASE_REASON_UNKNOWN, "A prisoner has been transferred from $PRISON_ID")
    awaitAtMost30Secs untilAsserted {
      verify(eventProcessingComplete).complete()
    }
    val apps = appRepository.findAppsByRequestedBy(PRISONER_NOMS_NUMBER)
    assertThat(apps.filter { it.status == AppStatus.REJECTED }).hasSize(2)
    assertThat(responseRepository.findAll()).hasSize(2)
    assertThat(historyRepository.findAll()).hasSizeGreaterThanOrEqualTo(2)

    verify(telemetryClient).trackEvent(
      eq("PRISONER_RELEASED"),
      argThat { map ->
        map["newPrisonerId"] == PRISONER_NOMS_NUMBER &&
          map["releaseReason"] == RELEASE_REASON_UNKNOWN &&
          map["createdBy"] == "MANAGE_APPS_ADMIN" &&
          map["status"] == "SUCCESS" &&
          map.containsKey("dateTime")
      },
      isNull(),
    )
    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(0)
  }

  @Test
  fun `should not process apps when release reason is not RELEASED or TRANSFERRED or UNKNOWN`() {
    appRepository.save(generateOpenApp1())
    assertThat(appRepository.findAppsByRequestedBy(PRISONER_NOMS_NUMBER)).hasSize(1)
    publishReleaseEvent("SENT_TO_COURT", "A prisoner sent to court")
    awaitAtMost30Secs untilAsserted {
      verify(eventProcessingComplete).complete()
    }
    // App status should remain NEW — no processing for unrecognised release reasons
    val apps = appRepository.findAppsByRequestedBy(PRISONER_NOMS_NUMBER)
    assertThat(apps.filter { it.status == AppStatus.NEW }).hasSize(1)
    verifyNoInteractions(telemetryClient)
    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(0)
  }

  @Test
  fun `should not track telemetry event when no open apps for prisoner`() {
    val nonExistentNomsNumber = "AX9999ZZ"
    assertThat(appRepository.findAppsByRequestedBy(nonExistentNomsNumber)).hasSize(0)
    publishReleaseEventForNoms(nonExistentNomsNumber, RELEASE_REASON_RELEASED, "A prisoner has been released from $PRISON_ID")
    awaitAtMost30Secs untilAsserted {
      verify(eventProcessingComplete).complete()
    }
    verifyNoInteractions(telemetryClient)
    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(0)
  }

  private fun purgeQueueSafely() {
    try {
      domainEventsQueue.sqsClient.purgeQueue {
        it.queueUrl(domainEventsQueue.queueUrl)
      }
      Thread.sleep(1000)
    } catch (e: Exception) {
      println("Queue purge skipped: ${e.message}")
    }
  }

  private fun publishReleaseEvent(reason: String, description: String) {
    publishReleaseEventForNoms(PRISONER_NOMS_NUMBER, reason, description)
  }

  private fun publishReleaseEventForNoms(nomsNumber: String, reason: String, description: String) {
    val domainEvent = HMPPSReleaseDomainEvent(
      eventType = PRISONER_RELEASE_EVENT_TYPE,
      additionalInformation = AdditionalInformation(
        nomsNumber = nomsNumber,
        reason = reason,
        prisonId = PRISON_ID,
      ),
      occurredAt = Instant.now().toString(),
      description = description,
      version = "1.0",
    )

    val messageJson = jsonString(domainEvent)

    domainEventsTopic.snsClient.publish {
      it.topicArn(domainEventsTopic.arn)
        .message(messageJson)
        .messageAttributes(
          mapOf(
            "eventType" to software.amazon.awssdk.services.sns.model.MessageAttributeValue.builder()
              .dataType("String")
              .stringValue(PRISONER_RELEASE_EVENT_TYPE)
              .build(),
          ),
        )
    }
  }
  private fun generateOpenApp1(): App = generateAppForMerge(
    id = UUID.fromString("33333333-3333-3333-3333-333333333331"),
    reference = "REL-001",
    assignedGroup = assignedGroup,
    appType = AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT,
    applicationGroup = 1,
    applicationType = 1,
    requestedDate = LocalDateTime.of(2026, 1, 1, 10, 0, 0),
    requestedBy = PRISONER_NOMS_NUMBER,
    requestedByFirstName = "John",
    requestedByLastName = "Doe",
    status = AppStatus.NEW,
    establishmentId = PRISON_ID,
    firstNightCenter = false,
  )

  private fun generateOpenApp2(): App = generateAppForMerge(
    id = UUID.fromString("33333333-3333-3333-3333-333333333332"),
    reference = "REL-002",
    assignedGroup = assignedGroup,
    appType = AppType.PIN_PHONE_EMERGENCY_CREDIT_TOP_UP,
    applicationGroup = 1,
    applicationType = 2,
    requestedDate = LocalDateTime.of(2026, 1, 2, 10, 0, 0),
    requestedBy = PRISONER_NOMS_NUMBER,
    requestedByFirstName = "John",
    requestedByLastName = "Doe",
    status = AppStatus.IN_PROGRESS,
    establishmentId = PRISON_ID,
    firstNightCenter = false,
  )

  private fun generateClosedApp(): App = generateAppForMerge(
    id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
    reference = "REL-003",
    assignedGroup = assignedGroup,
    appType = AppType.PIN_PHONE_ADD_NEW_OFFICIAL_CONTACT,
    applicationGroup = 1,
    applicationType = 1,
    requestedDate = LocalDateTime.of(2026, 1, 3, 10, 0, 0),
    requestedBy = PRISONER_NOMS_NUMBER,
    requestedByFirstName = "John",
    requestedByLastName = "Doe",
    status = AppStatus.APPROVED,
    establishmentId = PRISON_ID,
    firstNightCenter = false,
  )
}
