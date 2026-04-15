package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.stereotype.Component

/**
 * Component used in integration tests to signal when async event processing is complete.
 * In production, this is a no-op. In tests, it's mocked to verify processing completion.
 */
@Component
class EventProcessingComplete {
  fun complete() {
    // Called at the end of event processing
    // Used by tests to know when async processing is done
  }
}

