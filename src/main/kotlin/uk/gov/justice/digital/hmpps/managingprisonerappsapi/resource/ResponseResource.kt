package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

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
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppDecisionRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppDecisionResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.ResponseService
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import java.util.*

@RestController
@RequestMapping("v1")
class ResponseResource(val responseService: ResponseService) {

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
    val entity = responseService.addResponse(prisonerId, appId, authentication.principal, appDecisionRequestDto)
    return ResponseEntity.status(HttpStatus.CREATED).body(entity)
  }

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
    authentication as AuthAwareAuthenticationToken
    val entity = responseService.getResponseById(prisonerId, appId, authentication.principal, createdBy, responseId)
    return ResponseEntity.status(HttpStatus.OK).body(entity)
  }
}
