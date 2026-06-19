package uk.gov.justice.digital.hmpps.managingprisonerappsapi.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String? = buildProperties.version

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("https://managing-prisoner-apps-api-dev.hmpps.service.justice.gov.uk").description("Development"),
        Server().url("https://managing-prisoner-apps-api-preprod.hmpps.service.justice.gov.uk").description("Pre-Production"),
        Server().url("https://managing-prisoner-apps-api.hmpps.service.justice.gov.uk").description("Production"),
        Server().url("http://localhost:8080").description("Local"),
      ),
    )
    .info(
      Info().title("HMPPS Managing Prisoner Apps Api").version(version)
        .contact(Contact().name("HMPPS Digital Studio").email("feedback@digital.justice.gov.uk")),
    )
    .components(
      Components()
        .addSecuritySchemes(
          "MANAGING_PRISONER_APPS",
          SecurityScheme().addBearerJwtRequirement("ROLE_MANAGING_PRISONER_APPS"),
        )
        .addSecuritySchemes(
          "PRISONER_FACING_APPS",
          SecurityScheme().addBearerJwtRequirement("ROLE_PRISONER_FACING_APPS"),
        ),
    )
    .addSecurityItem(SecurityRequirement().addList("MANAGING_PRISONER_APPS", listOf("read")))
    .addSecurityItem(SecurityRequirement().addList("PRISONER_FACING_APPS", listOf("read")))
}

private fun SecurityScheme.addBearerJwtRequirement(role: String): SecurityScheme = type(SecurityScheme.Type.HTTP)
  .scheme("bearer")
  .bearerFormat("JWT")
  .`in`(SecurityScheme.In.HEADER)
  .name("Authorization")
  .description("A HMPPS Auth access token with the `$role` role.")
