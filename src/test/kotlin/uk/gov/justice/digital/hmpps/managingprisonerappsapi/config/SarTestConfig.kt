package uk.gov.justice.digital.hmpps.managingprisonerappsapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "hmpps.sar.tests")
data class SarTestConfig(
  var generateActual: Boolean = false,
  var expectedApiResponsePath: String = "",
  var expectedJpaEntitySchemaPath: String = "",
  var expectedRenderResultPath: String = "",
  var attachmentsExpected: Boolean = true,
  var expectedFlywaySchemaVersion: String = "0",
)
