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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.AppsSearchQueryDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.CommentRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppResponseListDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.HistoryResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.RequestedByNameSearchResult
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.AppService
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.*

@RestController
@RequestMapping("v1")
class AppResource(var appService: AppService) {

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostMapping(
    "prisoners/{prisonerId}/apps",
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
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  fun submitApp(
    @PathVariable("prisonerId") prisonerId: String,
    @RequestBody appRequestDto: AppRequestDto,
    authentication: Authentication,
  ): ResponseEntity<AppResponseDto<Any, Any>> {
    authentication as AuthAwareAuthenticationToken
    logger.info("Request received for submitting app for $prisonerId by ${authentication.principal}")
    val appResponseDto = appService.submitApp(prisonerId, authentication.principal, appRequestDto)
    return ResponseEntity.status(HttpStatus.CREATED).body(appResponseDto)
  }

  @Tag(name = "Apps")
  @Operation(
    summary = "Update App request form data for a prisoner",
    description = "This api endpoint is for updating app request for a prisoner. The logged staff and prisoner should belongs to same establishment. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "App form data updated"),
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
  @PutMapping(
    "prisoners/{prisonerId}/apps/{appId}",
    produces = [MediaType.APPLICATION_JSON_VALUE],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  fun updateAppRequestData(
    @PathVariable prisonerId: String,
    @PathVariable appId: UUID,
    @RequestBody appFormData: List<Map<String, Any>>,
    authentication: Authentication,
  ): ResponseEntity<AppResponseDto<Any, Any>> {
    authentication as AuthAwareAuthenticationToken
    logger.info("Request received for updating app requests data for $prisonerId by ${authentication.principal}")
    val appResponseDto = appService.updateAppFormData(prisonerId, authentication.principal, appId, appFormData)
    return ResponseEntity.status(HttpStatus.OK).body(appResponseDto)
  }

  @GetMapping("/prisoners/{prisonerId}/apps/{id}")
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
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  fun getAppById(
    @PathVariable prisonerId: String,
    @PathVariable id: UUID,
    @RequestParam(required = false) requestedBy: Boolean,
    @RequestParam(required = false) assignedGroup: Boolean,
    authentication: Authentication,
  ): ResponseEntity<AppResponseDto<Any, Any>> {
    authentication as AuthAwareAuthenticationToken
    logger.info("Request received for get app for $prisonerId by ${authentication.principal}")
    val appResponseDto = appService.getAppsById(prisonerId, id, authentication.principal, requestedBy, assignedGroup)
    return ResponseEntity.status(HttpStatus.OK).body(appResponseDto)
  }

  @Tag(name = "Apps")
  @Operation(
    summary = "Get all activity associated with an app.",
    description = "This api endpoint to get all activities associated with an app by app id. The logged staff and prisoner should belongs to same establishment. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "All activity associated with processing app request provided in response."),
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
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  @GetMapping("/prisoners/{prisonerId}/apps/{id}/history")
  fun getHistoryByAppId(
    @PathVariable prisonerId: String,
    @PathVariable id: UUID,
    authentication: Authentication,
  ): ResponseEntity<List<HistoryResponse>> {
    authentication as AuthAwareAuthenticationToken
    logger.info("Request received for get app for $prisonerId by ${authentication.principal}")
    val appResponseDto = appService.getHistoryAppsId(prisonerId, id, authentication.principal)
    return ResponseEntity.status(HttpStatus.OK).body(appResponseDto)
  }

  @Tag(name = "Apps")
  @Operation(
    summary = "Forward app to another group.",
    description = "This api endpoint is forwarding app to a different group. The logged staff and prisoner to which this app belongs should have same establishment. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "App successfully forwarded to another group"),
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
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  @PostMapping(
    "/apps/{appId}/forward/groups/{groupId}",
    produces = [MediaType.APPLICATION_JSON_VALUE],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun forwardAppToGroup(
    @PathVariable groupId: UUID,
    @PathVariable appId: UUID,
    authentication: Authentication,
    @RequestBody commentRequestDto: CommentRequestDto?,
  ): ResponseEntity<AppResponseDto<Any, Any>> {
    authentication as AuthAwareAuthenticationToken
    logger.info("Request received for to forward app to $groupId by ${authentication.principal}")
    val app = appService.forwardAppToGroup(authentication.principal, groupId, appId, commentRequestDto)
    return ResponseEntity.status(HttpStatus.OK).body(app)
  }

  @Tag(name = "Apps")
  @Operation(
    summary = "Search apps by search filter",
    description = "This api endpoint is for searching apps by using search filter which are app status, assigned group, app type, prisoner id and establishment. The logged staff can search only apps which belongs to staff establishment. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "List of apps based on search filter parameter values"),
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
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  @PostMapping(
    "/prisoners/apps/search",
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getAppsBySearchFilter(
    @RequestBody appsSearchQueryDto: AppsSearchQueryDto,
    authentication: Authentication,
  ): ResponseEntity<AppResponseListDto> {
    logger.info("Request received for to search apps")
    authentication as AuthAwareAuthenticationToken
    if (appsSearchQueryDto.types != null && appsSearchQueryDto.types!!.isEmpty()) {
      appsSearchQueryDto.types = null
    }
    if (appsSearchQueryDto.status.isEmpty()) {
      throw ApiException("Status cannot be empty", HttpStatus.BAD_REQUEST)
    }
    if (appsSearchQueryDto.assignedGroups != null && appsSearchQueryDto.assignedGroups!!.isEmpty()) {
      appsSearchQueryDto.assignedGroups = null
    }
    val appResponseDto = appService.searchAppsByColumnsFilter(
      authentication.principal,
      appsSearchQueryDto.status,
      appsSearchQueryDto.types,
      appsSearchQueryDto.requestedBy,
      appsSearchQueryDto.assignedGroups,
      appsSearchQueryDto.page,
      appsSearchQueryDto.size,
    )
    return ResponseEntity.status(HttpStatus.OK).body(appResponseDto)
  }

  @Tag(name = "Apps")
  @Operation(
    summary = "Search prisoners by name either first or lastname or both",
    description = "This api endpoint is for searching prisoners for whom the app request has been submitted. The user can see only the prisoners name for their own establishment only. The logged staff and prisoner to which this app belongs should have same establishment. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "App successfully forwarded to another group"),
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
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  @GetMapping(
    "/prisoners/search",
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getRequestedByTextSearch(
    @RequestParam name: String,
    authentication: Authentication,
  ): ResponseEntity<List<RequestedByNameSearchResult>> {
    logger.info("Request received for to search requested by text:$name")
    authentication as AuthAwareAuthenticationToken
    val searchResult = appService.searchRequestedByTextSearch(authentication.principal, name)
    return ResponseEntity.status(HttpStatus.OK).body(searchResult)
  }
}
