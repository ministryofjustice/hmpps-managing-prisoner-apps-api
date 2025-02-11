package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AppResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.AppService
import java.util.UUID

@RestController
@RequestMapping("v1")
class AppController(var appService: AppService) {

  @PostMapping("prisoners/{}prisoner-id/apps")
  fun submitApp(
    @RequestPart("prisoner-id") prisonerId: String,
    @RequestBody appRequestDto: AppRequestDto): ResponseEntity<AppResponseDto> {
    val appResponseDto = appService.submitApp(prisonerId, UUID.randomUUID().toString(), appRequestDto)
    return ResponseEntity.status(HttpStatus.OK).body(appResponseDto)
  }

  fun updateApp(@RequestBody appResponseDto: AppResponseDto): ResponseEntity<AppResponseDto> {
    return ResponseEntity.status(HttpStatus.CREATED).build()

  }

  fun getAppById(@RequestBody appResponseDto: AppResponseDto): ResponseEntity<AppResponseDto> {
    return ResponseEntity.status(HttpStatus.OK).build()
  }

  fun getAppsByEstablishment(@RequestBody appResponseDto: AppResponseDto): ResponseEntity<AppResponseDto> {
    return ResponseEntity.status(HttpStatus.OK).build()
  }
}