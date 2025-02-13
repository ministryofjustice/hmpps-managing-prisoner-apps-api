package uk.gov.justice.digital.hmpps.managingprisonerappsapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer

// @Configuration
class SecurityConfig {
  @Bean
  fun webSecurityCustomizer(): WebSecurityCustomizer? {
    return WebSecurityCustomizer { web: WebSecurity -> web.ignoring().requestMatchers("/v1/**") }
  }
}