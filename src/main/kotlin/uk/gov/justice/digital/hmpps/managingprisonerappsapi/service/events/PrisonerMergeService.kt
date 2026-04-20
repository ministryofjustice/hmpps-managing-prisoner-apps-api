package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.events

interface PrisonerMergeService {

  fun mergePrisonerNomsNumbers(mergedNomsNumber: String, removedNomsNumber: String, description: String)
}
