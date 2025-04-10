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
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppResponseListDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppsSearchQueryDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.RequestedByNameSearchResult
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.AppService
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
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

  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  @GetMapping("/prisoners/{prisonerId}/apps/{id}")
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

  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  @GetMapping("/apps/{appId}/forward/groups/{groupId}")
  fun forwardAppToGroup(
    @PathVariable groupId: UUID,
    @PathVariable appId: UUID,
    authentication: Authentication,
  ): ResponseEntity<AppResponseDto<Any, Any>> {
    authentication as AuthAwareAuthenticationToken
    logger.info("Request received for to forward app to $groupId by ${authentication.principal}")
    val app = appService.forwardAppToGroup(authentication.principal, groupId, appId)
    return ResponseEntity.status(HttpStatus.OK).body(app)
  }

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

  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  @GetMapping(
    "/prisoners/search",
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getRequestedByTextSearch(
    @RequestParam name: String,
    authentication: Authentication,
  ): ResponseEntity<List<RequestedByNameSearchResult>> {
    authentication as AuthAwareAuthenticationToken
    val searchResult = appService.searchRequestedByTextSearch(authentication.principal, name)
    return ResponseEntity.status(HttpStatus.OK).body(searchResult)
  }
}
