package uk.gov.justice.digital.hmpps.managingprisonerappsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableConfigurationProperties
@EnableCaching
class ManagingPrisonerAppsApi

fun main(args: Array<String>) {
  runApplication<ManagingPrisonerAppsApi>(*args)
}
