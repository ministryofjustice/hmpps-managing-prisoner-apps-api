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
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppDecisionRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppDecisionResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.ResponseService
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.*

@RestController
@RequestMapping("v1")
class ResponseResource(val responseService: ResponseService) {

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Tag(name = "Responses")
  @Operation(
    summary = "Add a response for a app request.",
    description = "This api endpoint is for adding response to an app request. The logged staff and prisoner should belongs to same establishment." +
      " Currently only one request is supported in per app so there is only one approval or decline." +
      " Requires role ROLE_MANAGING_PRISONER_APPS",
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
  @PostMapping(
    "prisoners/{prisonerId}/apps/{appId}/responses",
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  fun addResponse(
    @PathVariable prisonerId: String,
    @PathVariable appId: UUID,
    @RequestBody appDecisionRequestDto: AppDecisionRequestDto,
    authentication: Authentication,
  ): ResponseEntity<AppDecisionResponseDto<Any>> {
    authentication as AuthAwareAuthenticationToken
    logger.info("Request received to add response by ${authentication.principal}")
    val entity = responseService.addResponse(prisonerId, appId, authentication.principal, appDecisionRequestDto)
    return ResponseEntity.status(HttpStatus.CREATED).body(entity)
  }

  @Tag(name = "Responses.")
  @Operation(
    summary = "Get response by id.",
    description = "This api endpoint is for getting response addy by prison staff for prisoner's app request.. The logged staff and prisoner should belongs to same establishment. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "App response returned in response successfully."),
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
  @GetMapping(
    "prisoners/{prisonerId}/apps/{appId}/responses/{responseId}",
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  fun getResponseById(
    @PathVariable prisonerId: String,
    @PathVariable appId: UUID,
    @PathVariable responseId: UUID,
    @RequestParam(required = false) createdBy: Boolean = false,
    authentication: Authentication,
  ): ResponseEntity<AppDecisionResponseDto<Any>> {
    logger.info("Request received to get response for $responseId by ${authentication.principal}")
    authentication as AuthAwareAuthenticationToken
    val entity = responseService.getResponseById(prisonerId, appId, authentication.principal, createdBy, responseId)
    return ResponseEntity.status(HttpStatus.OK).body(entity)
  }
}
