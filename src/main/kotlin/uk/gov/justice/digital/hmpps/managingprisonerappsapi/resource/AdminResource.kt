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
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.EstablishmentApplicationTypeRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentApplicationTypeResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.EstablishmentApplicationTypeService
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("v1/admin/")
class AdminResource(
  var establishmentApplicationTypeService: EstablishmentApplicationTypeService,
) {

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Tag(name = "Admin")
  @Operation(
    summary = "Get All Application Types by Establishment",
    description = "This api endpoint is for getting all application types by establishment. The logged staff " +
      " must have the role MANAGING_PRISONER_APPS and be associated with an establishment.",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "Successfully retrieved all application types for the staff's establishment"),
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
    "department-mappings",
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  fun getAllApplicationTypes(
    authentication: Authentication,
  ): ResponseEntity<List<EstablishmentApplicationTypeResponseDto>> {
    authentication as AuthAwareAuthenticationToken
    logger.info("Request received for getting all application types by establishment by ${authentication.principal}")
    val applicationTypes = establishmentApplicationTypeService.getAllApplicationTypesByEstablishment(authentication.principal)
    return ResponseEntity.status(HttpStatus.OK).body(applicationTypes)
  }

  @Tag(name = "Admin")
  @Operation(
    summary = "Update Application Types by Establishment",
    description = "This api endpoint is for updating application types by establishment. The logged staff " +
      " must have the role MANAGING_PRISONER_APPS and be associated with an establishment.",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "Successfully updated all application types for the staff's establishment"),
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
  @PatchMapping(
    "department-mappings",
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  fun updateApplicationTypes(
    authentication: Authentication,
    @RequestBody establishmentApplicationTypeRequestDtoList: List<EstablishmentApplicationTypeRequestDto>,
  ): ResponseEntity<List<EstablishmentApplicationTypeResponseDto>> {
    authentication as AuthAwareAuthenticationToken
    logger.info("Request received for updating all application types by establishment by ${authentication.principal}")
    val applicationTypes = establishmentApplicationTypeService.saveEstablishmentApplicationTypes(authentication.principal, establishmentApplicationTypeRequestDtoList)
    return ResponseEntity.status(HttpStatus.OK).body(applicationTypes)
  }
}
