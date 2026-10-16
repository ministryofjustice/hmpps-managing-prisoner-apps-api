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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.ApplicationGroupResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.EstablishmentApplicationTypeService
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.EstablishmentService
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("")
class EstablishmentResource(
  private val establishmentService: EstablishmentService,
  private val establishmentApplicationTypeService: EstablishmentApplicationTypeService,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Tag(name = "Establishments")
  @Operation(
    summary = "Get an establishment by agency id",
    description = "This api endpoint is for getting an establishment by agency id. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "Establishment add successfully"),
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
  @GetMapping("/v1/establishments/{id}")
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS', 'PRISON')")
  fun getEstablishmentById(@PathVariable id: String): ResponseEntity<EstablishmentDto> {
    val establishmentDto = establishmentService.getEstablishmentById(id)
    if (establishmentDto.isEmpty) {
      throw ApiException("No establishment with id $id", HttpStatus.BAD_REQUEST)
    }
    return ResponseEntity.status(HttpStatus.OK).body(establishmentDto.get())
  }

  @Tag(name = "Establishments")
  @Operation(
    summary = "Get list of  establishment.",
    description = "This api endpoint is for getting establishment list using manage prisoner apps. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "Establishment add successfully"),
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
  @GetMapping("/v1/establishments")
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS', 'PRISON')")
  fun getEstablishments(): ResponseEntity<Set<String>> {
    val establishments = establishmentService.getEstablishments()
    return ResponseEntity.status(HttpStatus.OK).body(establishments)
  }

  /**
   * Uses new establishmentApplication Group table
   */
  @Tag(name = "Establishments")
  @Operation(
    summary = "Get application groups and types by logged in user's establishment",
    description = "This api endpoint is for getting an application groups and types by establishment. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "Establishment add successfully"),
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
  @GetMapping("/v1/establishments/apps/groups", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS', 'PRISON')")
  fun getAppGroupsForEstablishment(authentication: Authentication): ResponseEntity<List<ApplicationGroupResponse>> {
    authentication as AuthAwareAuthenticationToken
    logger.info("Request received for get app types & Groups for ${authentication.principal}")
    val appTypes = establishmentApplicationTypeService.getActiveApplicationTypesByStaffId(authentication.principal)
    return ResponseEntity.status(HttpStatus.OK).body(appTypes)
  }
}
