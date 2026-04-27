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
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppResponsePrisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.ApplicationGroupResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.ApplicationTypeResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrisonerAppsPage
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.AppPrisonerFacingService
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.*

@RestController
@RequestMapping("v1")
class PrisonerFacingResource(private val appPrisonerFacingService: AppPrisonerFacingService) {

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Tag(name = "Prisoner Apps")
  @Operation(
    summary = "Get apps for  a prisoner",
    description = "This api endpoint to get prisoner apps. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "Successfully got prisoner apps."),
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
  @GetMapping("/prisoners/apps", produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getPrisonerApps(
    @RequestParam(value = "pageNum", required = true) pageNum: Long,
    @RequestParam(value = "pageSize", required = false) pageSize: Long? = 20,
    authentication: Authentication,
  ): ResponseEntity<PrisonerAppsPage> {
    logger.info("Request received for getting apps for prisoner: ${authentication.principal}")
    authentication as AuthAwareAuthenticationToken
    val apps = appPrisonerFacingService.getAppsByPrisonerId(authentication.principal, pageNum, pageSize!!)
    return ResponseEntity.status(HttpStatus.OK).body(apps)
  }

  @Tag(name = "Prisoner Apps")
  @Operation(
    summary = "Get app by app id for a logged prisoner",
    description = "This api endpoint to get prisoner app by app id . Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "Successfully got app by app id."),
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
  @GetMapping("/prisoners/apps/{id}")
  fun getPrisonerAppByAppId(
    @PathVariable("id") id: UUID,
    authentication: Authentication,
  ): ResponseEntity<AppResponsePrisoner<Any, Any>> {
    logger.info("Request received for getting app by id for prisoner: ${authentication.principal}")
    authentication as AuthAwareAuthenticationToken
    val appResponseDto = appPrisonerFacingService.getPrisonerAppById(authentication.principal, id)
    return ResponseEntity.status(HttpStatus.OK).body(appResponseDto)
  }

  @Tag(name = "Prisoner Apps")
  @Operation(
    summary = "Get app groups and app types.",
    description = "This api endpoint to app groups and app types for a logged prisoner. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "App request created."),
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
  @GetMapping("/prisoners/apps/groups")
  fun getPrisonerAppTypes(
    authentication: Authentication,
  ): ResponseEntity<List<ApplicationGroupResponse>> {
    authentication as AuthAwareAuthenticationToken
    val appResponseDto = appPrisonerFacingService.getAppGroupsAndTypesByLoggedUserEstablishment(authentication.principal)
    return ResponseEntity.status(HttpStatus.OK).body(appResponseDto)
  }

  @Tag(name = "Prisoner Apps")
  @Operation(
    summary = "Get app count in pending status by application type",
    description = "This api endpoint to app groups and app types for a logged prisoner. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "App request created."),
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
  @GetMapping("/prisoners/apps/{applicationType}/pending")
  fun getPrisonerPendingAppCountByAppType(
    @PathVariable("applicationType") applicationType: Long,
    authentication: Authentication,
  ): ResponseEntity<ApplicationTypeResponse> {
    logger.info("Request received for getting apps count in pending status for prisoner: ${authentication.principal} with app type: $applicationType")
    authentication as AuthAwareAuthenticationToken
    val appResponseDto = appPrisonerFacingService.getPrisonerAppsCountInPending(authentication.principal, applicationType)
    return ResponseEntity.status(HttpStatus.OK).body(appResponseDto)
  }

  @Tag(name = "Prisoner Apps")
  @Operation(
    summary = "Submit App request for a prisoner",
    description = "This api endpoint is for submitting app request by  a logged prisoner.  Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "201", description = "App request submitted"),
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
  @PostMapping(
    "prisoners/apps",
    produces = [MediaType.APPLICATION_JSON_VALUE],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS', 'PRISON')")
  fun submitApp(
    @RequestBody appRequestPrisoner: AppRequestPrisoner,
    authentication: Authentication,
  ): ResponseEntity<AppResponsePrisoner<Any, Any>> {
    authentication as AuthAwareAuthenticationToken
    logger.info("Request received for submitting app request by Prisoner: ${authentication.principal}")
    val appResponseDto = appPrisonerFacingService.submitApp(appRequestPrisoner, authentication.principal)
    return ResponseEntity.status(HttpStatus.CREATED).body(appResponseDto)
  }
}
