package uk.gov.justice.digital.hmpps.managingprisonerappsapi.config

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.context.annotation.Bean

class TelemetryConfig { @Bean
fun getTelemetryClient(): TelemetryClient = TelemetryClient()

}