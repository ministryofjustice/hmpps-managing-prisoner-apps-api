package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

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
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.AppService
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import java.util.*

@RestController
@RequestMapping("v1")
class AppController(var appService: AppService) {

  companion object {
    private val logger = LoggerFactory.getLogger(AppController::class.java)
  }

  @PostMapping(
    "prisoners/{prisoner-id}/apps",
    produces = [MediaType.APPLICATION_JSON_VALUE],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyRole('SAR_DATA_ACCESS')")
  fun submitApp(
    @PathVariable("prisoner-id") prisonerId: String,
    @RequestBody appRequestDto: AppRequestDto,
    authentication: Authentication,
  ): ResponseEntity<AppResponseDto> {
    authentication as AuthAwareAuthenticationToken
    logger.info("Request received for submitting app for $prisonerId by ${authentication.principal}")
    val appResponseDto = appService.submitApp(prisonerId, authentication.principal, appRequestDto)
    return ResponseEntity.status(HttpStatus.CREATED).body(appResponseDto)
  }

  fun updateApp(@RequestBody appResponseDto: AppResponseDto): ResponseEntity<AppResponseDto> = ResponseEntity.status(HttpStatus.CREATED).build()

  @PreAuthorize("hasAnyRole('SAR_DATA_ACCESS')")
  @GetMapping("/prisoners/{prisoner-id}/apps/{id}")
  fun getAppById(
    @PathVariable("prisoner-id") prisonerId: String,
    @PathVariable("id") id: UUID,
    @RequestParam(required = false) requestedBy: Boolean,
    @RequestParam(required = false) assignedGroup: Boolean,
    authentication: Authentication,
  ): ResponseEntity<AppResponseDto> {
    authentication as AuthAwareAuthenticationToken
    logger.info("Request received for get app for $prisonerId by ${authentication.principal}")
    val appResponseDto = appService.getAppsById(prisonerId, id, requestedBy, assignedGroup)
    return ResponseEntity.status(HttpStatus.OK).body(appResponseDto)
  }

  @PreAuthorize("hasAnyRole('SAR_DATA_ACCESS')")
  @GetMapping("/apps/{appId}/groups/{groupId}")
  fun forwardAppToGroup(groupId: UUID, appId: UUID, authentication: Authentication): ResponseEntity<AppResponseDto> {
    authentication as AuthAwareAuthenticationToken
    logger.info("Request received for to forward app to $groupId by ${authentication.principal}")
    val app = appService.forwardAppToGroup(groupId, appId)
    return ResponseEntity.status(HttpStatus.OK).body(app)
  }

  fun getAppsBySearchFilter(
    @RequestParam(required = false) prisonerId: String?,
    @RequestParam(required = false) groupId: UUID?,
    @RequestParam(required = false) appType: AppType?,
    @RequestParam(required = true) status: String?,
  ): ResponseEntity<AppResponseDto> {
    if (status != "pending" && status != "closed") {
      throw ApiException("Status can be either pending or closed", HttpStatus.BAD_REQUEST)
    }
    return ResponseEntity.status(HttpStatus.OK).build()
  }

}
