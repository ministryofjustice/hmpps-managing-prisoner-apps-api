package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.manage.users

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class UserDetailsDto(
  @Schema(description = "username", example = "DEMO_USER1", required = true)
  @JsonProperty("username")
  @field:NotNull
  val username: String,

  @Schema(description = "Full name", example = "John Smith", required = false)
  @JsonProperty("name")
  val fullName: String,

  @Schema(description = "Active", example = "true", required = false)
  @JsonProperty("active")
  val active: Boolean? = false,

  @Schema(description = "authSource", example = "nomis", required = false)
  @JsonProperty("authSource")
  val authSource: String? = null,

  @Schema(description = "staffId", example = "488028", required = false)
  @JsonProperty("staffId")
  val staffId: Long? = null,

  @Schema(description = "Active caseload", example = "activeCaseLoadId", required = false)
  @JsonProperty("activeCaseLoadId")
  val activeCaseLoadId: String,

  @Schema(description = "Staff Id", example = "488028", required = false)
  @JsonProperty("userId")
  val userId: String,

  @Schema(description = "UUId", example = "d6c60208-6508-4823-b664-0674ca06ddea", required = false)
  @JsonProperty("uuid")
  val uuid: UUID? = null,
)
