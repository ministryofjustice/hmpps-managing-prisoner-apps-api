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
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AssignedGroupDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.EstablishmentDepartmentService
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("v1")
class DepartmentResource(
  private val establishmentDepartmentService: EstablishmentDepartmentService,
) {

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Tag(name = "Departments")
  @Operation(
    summary = "Get list of active departments enable for an establishment.",
    description = "This api endpoint is for getting list of all department enabled to handle request for a given application type. " +
      "Requires role ROLE_MANAGING_PRISONER_APPS",
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
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS', 'PRISON')")
  @GetMapping("/groups/app/types/{type}", produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getActiveDepartmentsForEstablishment(
    authentication: Authentication,
    @PathVariable("type") type: Long,
  ): ResponseEntity<List<AssignedGroupDto>> {
    authentication as AuthAwareAuthenticationToken
    logger.info("Request received to get groups by ${authentication.principal}")
    val departments = establishmentDepartmentService.findActiveDepartmentsForEstablishment(authentication.principal)
    return ResponseEntity.status(HttpStatus.OK).body(departments)
  }
}
