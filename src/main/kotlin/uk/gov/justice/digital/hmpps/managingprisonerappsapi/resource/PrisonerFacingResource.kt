package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppListPrisonerFacing
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.AppServicePrisonerFacing

@RestController
@RequestMapping("v1")
class PrisonerFacingResource(private val appServicePrisonerFacing: AppServicePrisonerFacing) {

  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS', 'PRISON')")
  @GetMapping("/prisoners/{prisonerId}/apps", produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getAppsByPrisonerId(@PathVariable prisonerId: String): ResponseEntity<List<AppListPrisonerFacing>> {
    val apps = appServicePrisonerFacing.getAppsByPrisonerId(prisonerId)
    return ResponseEntity.status(HttpStatus.OK).body(apps)
  }
}
