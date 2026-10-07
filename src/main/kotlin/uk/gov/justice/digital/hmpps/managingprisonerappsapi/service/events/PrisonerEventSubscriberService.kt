package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.events

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.EventProcessingComplete

@Service
@ConditionalOnProperty(name = ["hmpps.sqs.enabled"], havingValue = "true")
class PrisonerEventSubscriberService(
  private val mapper: ObjectMapper,
  private val prisonerEventService: PrisonerEventService,
  private val eventProcessingComplete: EventProcessingComplete,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    const val PRISONER_MERGE_EVENT_TYPE = "prison-offender-events.prisoner.merged"
    const val PRISONER_RELEASE_EVENT_TYPE = "prison-offender-events.prisoner.released"
    const val PRISONER_RECEIVED_EVENT_TYPE = "prison-offender-events.prisoner.received"
    const val RELEASE_REASON_RELEASED = "RELEASED"
    const val RELEASE_REASON_TRANSFERRED = "TRANSFERRED"
  }

  @SqsListener("domaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun onPrisonerDomainEvent(requestJson: String) {
    var eventType: String = "unknown"
    try {
      val hmppsMessage = mapper.readValue(requestJson, HMPPSMessage::class.java)
      eventType = hmppsMessage.MessageAttributes.eventType.Value
      log.info("Received event type: $eventType")

      when (eventType) {
        PRISONER_MERGE_EVENT_TYPE -> {
          val mergeEvent = mapper.readValue(hmppsMessage.Message, HMPPSDomainEvent::class.java)
          log.info("Processing prisoner merge: ${mergeEvent.additionalInformation.removedNomsNumber} -> ${mergeEvent.additionalInformation.nomsNumber}")
          prisonerEventService.mergePrisonerNomsNumbers(
            mergeEvent.additionalInformation.nomsNumber,
            mergeEvent.additionalInformation.removedNomsNumber,
            mergeEvent.occurredAt,
          )
        }
        PRISONER_RELEASE_EVENT_TYPE -> {
          val releaseEvent = mapper.readValue(hmppsMessage.Message, HMPPSDomainEvent::class.java)
          log.info("Processing prisoner release: ${releaseEvent.additionalInformation.nomsNumber}, Reason: ${releaseEvent.additionalInformation.reason}")

          if (releaseEvent.additionalInformation.reason == RELEASE_REASON_RELEASED ||
            releaseEvent.additionalInformation.reason == RELEASE_REASON_TRANSFERRED
          ) {
            prisonerEventService.handlePrisonerReleased(
              releaseEvent.additionalInformation.nomsNumber,
              releaseEvent.additionalInformation.reason,
              releaseEvent.additionalInformation.prisonId,
              releaseEvent.occurredAt,
            )
          }
        }
        PRISONER_RECEIVED_EVENT_TYPE -> {
          val receivedEvent = mapper.readValue(hmppsMessage.Message, HMPPSDomainEvent::class.java)
          log.info("Processing prisoner received: ${receivedEvent.additionalInformation.nomsNumber}")
          prisonerEventService.handlePrisonerReceived(
            receivedEvent.additionalInformation.nomsNumber,
            receivedEvent.additionalInformation.reason,
            receivedEvent.additionalInformation.prisonId,
            receivedEvent.occurredAt,
          )
        }
        else -> {
          log.debug("Ignoring message with type $eventType")
        }
      }

      eventProcessingComplete.complete()
    } catch (e: Exception) {
      log.error("Error processing prisoner domain event $eventType", e)
      throw RuntimeException("Error processing prisoner domain event", e)
    }
  }
}

data class HMPPSDomainEvent(
  val eventType: String? = null,
  val additionalInformation: AdditionalInformation,
  val version: String,
  val occurredAt: String,
  val description: String,

)

data class AdditionalInformation(
  val nomsNumber: String,
  val removedNomsNumber: String,
  val reason: String,
  val prisonId: String,
)

// SNS notification wrapper - uses Pascal case as per AWS SNS format
data class HMPPSEventType(
  val Type: String, // Pascal case for SNS
  val Value: String, // Pascal case for SNS
)

data class HMPPSMessageAttributes(
  val eventType: HMPPSEventType,
)

data class HMPPSMessage(
  val Message: String, // Pascal case for SNS
  val MessageAttributes: HMPPSMessageAttributes, // Pascal case for SNS
)
