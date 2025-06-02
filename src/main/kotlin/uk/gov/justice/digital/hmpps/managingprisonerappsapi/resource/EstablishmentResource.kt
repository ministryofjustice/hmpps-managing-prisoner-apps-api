package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.EstablishmentService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("v1/")
class EstablishmentResource(private val establishmentService: EstablishmentService) {

  @Tag(name = "Establishments")
  @Operation(
    summary = "Add an establishment",
    description = "This api endpoint is for adding an establishment with agency id and name. Requires role ROLE_MANAGING_PRISONER_APPS",
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
  @PostMapping("establishments")
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  fun addEstablishment(@RequestBody establishmentDto: EstablishmentDto): ResponseEntity<EstablishmentDto> {
    establishmentService.saveEstablishment(establishmentDto)
    return ResponseEntity.ok(establishmentDto)
  }

  @Tag(name = "Establishments")
  @Operation(
    summary = "Update a establishment",
    description = "This api endpoint is for updating an establishment. Requires role ROLE_MANAGING_PRISONER_APPS",
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
  @PutMapping("establishments/{id}")
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  fun updateEstablishment(
    @PathVariable id: String,
    @RequestBody establishmentDto: EstablishmentDto,
  ): ResponseEntity<EstablishmentDto> {
    if (id != establishmentDto.id) {
      throw ApiException("Invalid id ${establishmentDto.id}", HttpStatus.BAD_REQUEST)
    }
    val establishment = establishmentService.updateEstablishment(establishmentDto)
    return ResponseEntity.status(HttpStatus.OK).body(establishment)
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
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  fun getEstablishmentById(@PathVariable id: String): ResponseEntity<EstablishmentDto> {
    val establishmentDto = establishmentService.getEstablishmentById(id)
    if (establishmentDto.isEmpty) {
      throw ApiException("No establishment with id $id", HttpStatus.BAD_REQUEST)
    }
    return ResponseEntity.status(HttpStatus.OK).body(establishmentDto.get())
  }
}
