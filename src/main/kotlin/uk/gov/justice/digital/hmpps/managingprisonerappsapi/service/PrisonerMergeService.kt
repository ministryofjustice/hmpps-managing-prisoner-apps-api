package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

interface PrisonerMergeService {

  fun mergePrisonerNomsNumbers(mergedNomsNumber: String, removedNomsNumber: String, description: String)
}
