package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppRequestPrisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppListPrisonerFacing
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppResponsePrisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppResponsePrisonerFacing
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.AppServicePrisonerFacing
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@RequestMapping("v1")
class PrisonerFacingResource(private val appServicePrisonerFacing: AppServicePrisonerFacing) {

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS', 'PRISON')")
  @GetMapping("/prisoners/apps", produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getAppsByPrisonerId(@PathVariable prisonerId: String, authentication: Authentication): ResponseEntity<List<AppListPrisonerFacing>> {
    authentication as AuthAwareAuthenticationToken
    val apps = appServicePrisonerFacing.getAppsByPrisonerId(prisonerId)
    return ResponseEntity.status(HttpStatus.OK).body(apps)
  }

  @GetMapping("/prisoners/apps/{id}")
  @Tag(name = "Apps")
  @Operation(
    summary = "Get app by id for a prisoner",
    description = "This api endpoint to get prisoner app. The logged staff and prisoner should belongs to same establishment. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "Successfully got app by id."),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS', 'PRISON')")
  fun getPrisonerAppById(
    @PathVariable("id") id: UUID,
    authentication: Authentication,
  ): ResponseEntity<AppResponsePrisoner<Any, Any>> {
    authentication as AuthAwareAuthenticationToken
    val appResponseDto = appServicePrisonerFacing.getPrisonerAppById(authentication.principal, id)
    return ResponseEntity.status(HttpStatus.OK).body(appResponseDto)
  }

  @PostMapping(
    "prisoners/apps",
    produces = [MediaType.APPLICATION_JSON_VALUE],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Tag(name = "Apps")
  @Operation(
    summary = "Submit App request for a prisoner",
    description = "This api endpoint is for submitting app request for a prisoner. The logged staff and prisoner should belongs to same establishment. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "App request submitted"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS', 'PRISON')")
  fun submitApp(
    @RequestBody appRequestPrisoner: AppRequestPrisoner,
    authentication: Authentication,
  ): ResponseEntity<AppResponsePrisoner<Any, Any>> {
    authentication as AuthAwareAuthenticationToken
    logger.info("Request received for submitting app for ${authentication.principal} by prisoner")
    val appResponseDto = appServicePrisonerFacing.submitApp(appRequestPrisoner, authentication.principal)
    return ResponseEntity.status(HttpStatus.CREATED).body(appResponseDto)
  }
}
