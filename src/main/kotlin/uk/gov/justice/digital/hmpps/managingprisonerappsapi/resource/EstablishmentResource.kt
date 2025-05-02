package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

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

@RestController
@RequestMapping("v1/")
class EstablishmentResource(private val establishmentService: EstablishmentService) {

  @PostMapping("establishments")
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  fun addEstablishment(@RequestBody establishmentDto: EstablishmentDto): ResponseEntity<EstablishmentDto> {
    establishmentService.saveEstablishment(establishmentDto)
    return ResponseEntity.ok(establishmentDto)
  }

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
