package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.AppService
import java.util.UUID

@RestController
@RequestMapping("v1")
class AppController(var appService: AppService) {

  @PostMapping("prisoners/{prisoner-id}/apps",
    produces = [MediaType.APPLICATION_JSON_VALUE],
    consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  fun submitApp(
    @PathVariable("prisoner-id") prisonerId: String,
    @RequestBody appRequestDto: AppRequestDto,
    authentication: Authentication,
    ): ResponseEntity<AppResponseDto> {
    val principal = authentication.principal as Jwt
    val staffId = principal.claims["username"] as String
    val appResponseDto = appService.submitApp(prisonerId, staffId, appRequestDto)
    return ResponseEntity.status(HttpStatus.CREATED).body(appResponseDto)
  }

  fun updateApp(@RequestBody appResponseDto: AppResponseDto): ResponseEntity<AppResponseDto> {
    return ResponseEntity.status(HttpStatus.CREATED).build()

  }

  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  @GetMapping("/prisoners/{prisoner-id}/apps/{id}")
  fun getAppById(@PathVariable("prisoner-id") prisonerId: String,
                 @PathVariable("id") id: UUID,
                 @RequestParam(required = false) requestedBy: Boolean,
                 @RequestParam(required = false) assignedGroup: Boolean,
                 ): ResponseEntity<AppResponseDto> {
    val appResponseDto = appService.getAppsById(prisonerId, id, requestedBy, assignedGroup)
    return ResponseEntity.status(HttpStatus.OK).body(appResponseDto)
  }

  fun getAppsByEstablishment(@RequestBody appResponseDto: AppResponseDto): ResponseEntity<AppResponseDto> {
    return ResponseEntity.status(HttpStatus.OK).build()
  }
}