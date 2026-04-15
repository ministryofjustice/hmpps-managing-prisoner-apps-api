package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PrisonerEventSubscriberService(
  private val mapper: ObjectMapper,
  private val prisonerMergeService: PrisonerMergeService,
  private val eventProcessingComplete: EventProcessingComplete,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    const val PRISONER_MERGE_EVENT_TYPE = "prison-offender-events.prisoner.merged"
  }

  @SqsListener("domaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun onPrisonerDomainEvent(requestJson: String) {
    try {
      val hmppsMessage = mapper.readValue(requestJson, HMPPSMessage::class.java)
      val eventType = hmppsMessage.messageAttributes.eventType.value
      log.info("Received message type: $eventType")

      when (eventType) {
        PRISONER_MERGE_EVENT_TYPE -> {
          val mergeEvent = mapper.readValue(hmppsMessage.message, HMPPSMergeDomainEvent::class.java)
          log.info("Processing prisoner merge: ${mergeEvent.additionalInformation.removedNomsNumber} -> ${mergeEvent.additionalInformation.nomsNumber}")
          prisonerMergeService.mergePrisonerNomsNumbers(
            mergeEvent.additionalInformation.nomsNumber,
            mergeEvent.additionalInformation.removedNomsNumber,
            mergeEvent.description,
          )
        }
        else -> {
          log.debug("Ignoring message with type $eventType")
        }
      }
      
      eventProcessingComplete.complete()
    } catch (e: Exception) {
      log.error("Error processing prisoner domain event", e)
      throw e
    }
  }
}

data class HMPPSMergeDomainEvent(
  val eventType: String? = null,
  val additionalInformation: AdditionalInformationMerge,
  val version: String,
  val occurredAt: String,
  val description: String,
)

data class AdditionalInformationMerge(
  val nomsNumber: String,
  val removedNomsNumber: String,
)

data class HMPPSEventType(val value: String, val type: String)
data class HMPPSMessageAttributes(val eventType: HMPPSEventType)
data class HMPPSMessage(
  val message: String,
  val messageAttributes: HMPPSMessageAttributes,
)
