package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentRepository
import java.util.*

@Service
class EstablishmentService(private val establishmentRepository: EstablishmentRepository) {

  fun saveEstablishment(establishment: Establishment): Establishment {
    return establishmentRepository.save(establishment)
  }

  fun updateEstablishment(establishment: Establishment): Establishment {
    val findEstablishment = establishmentRepository.findById(establishment.id).orElseThrow {
      ApiException("No existing establishment with id ${establishment.id}", HttpStatus.BAD_REQUEST)
    }
    return establishmentRepository.save(findEstablishment)
  }

  fun getEstablishmentById(id: String): Optional<Establishment> {
    return establishmentRepository.findById(id)
  }

  fun deleteEstablishmentById(id: String) {
    return establishmentRepository.deleteById(id)
  }

  fun convertEstablishmentToEstablishmentDto(establishment: Establishment): EstablishmentDto
    = EstablishmentDto(
      id = establishment.id,
      name = establishment.name,
      )

}
