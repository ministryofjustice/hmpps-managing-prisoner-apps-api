package uk.gov.justice.digital.hmpps.managingprisonerappsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ManagingPrisonerAppsApi

fun main(args: Array<String>) {
  runApplication<ManagingPrisonerAppsApi>(*args)
}
