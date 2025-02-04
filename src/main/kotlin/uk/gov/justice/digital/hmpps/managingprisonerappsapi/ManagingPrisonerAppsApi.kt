package uk.gov.justice.digital.hmpps.managingprisonerappsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties
class ManagingPrisonerAppsApi

fun main(args: Array<String>) {
  runApplication<ManagingPrisonerAppsApi>(*args)
}
