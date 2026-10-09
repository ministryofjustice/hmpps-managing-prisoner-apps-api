package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.events

interface PrisonerEventService {

  fun mergePrisonerNomsNumbers(mergedNomsNumber: String, removedNomsNumber: String, description: String)

  fun handlePrisonerReleased(nomsNumber: String, releaseReason: String, prisonId: String, eventTime: String)

  fun handlePrisonerReceived(nomsNumber: String, reason: String, prisonId: String, eventTime: String)
}
