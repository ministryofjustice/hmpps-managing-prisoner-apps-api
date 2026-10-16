package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentRepository
import java.util.*

@Service
class EstablishmentService(
  private val establishmentRepository: EstablishmentRepository,
) {

  fun getEstablishmentById(id: String): Optional<EstablishmentDto> {
    val establishment = establishmentRepository.findById(id)
    if (establishment.isPresent) {
      return Optional.of(convertEstablishmentToEstablishmentDto(establishment.get()))
    } else {
      return Optional.empty()
    }
  }

  fun getEstablishments(): Set<String> {
    val establishments: List<Establishment> = establishmentRepository.findAll()
    val list = HashSet<String>()
    establishments.forEach { establishment ->
      list.add(establishment.id)
    }
    return list
  }

  private fun convertEstablishmentToEstablishmentDto(establishment: Establishment): EstablishmentDto = EstablishmentDto(
    id = establishment.id,
    name = establishment.name,
    establishment.defaultDepartments,
  )
}
