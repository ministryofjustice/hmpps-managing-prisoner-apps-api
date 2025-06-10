package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentRepository
import java.util.*

@Service
class EstablishmentService(private val establishmentRepository: EstablishmentRepository) {

  fun saveEstablishment(establishmentDto: EstablishmentDto): EstablishmentDto {
    val establishment = convertEstablishmentDtoToEstablishment(establishmentDto)
    val entity = establishmentRepository.save(establishment)
    return convertEstablishmentToEstablishmentDto(entity)
  }

  fun updateEstablishment(establishmentDto: EstablishmentDto): EstablishmentDto {
    val findEstablishment = establishmentRepository.findById(establishmentDto.id).orElseThrow {
      ApiException("No existing establishment with id ${establishmentDto.id}", HttpStatus.BAD_REQUEST)
    }
    val establishment = establishmentRepository.save(convertEstablishmentDtoToEstablishment(establishmentDto))
    return convertEstablishmentToEstablishmentDto(establishment)
  }

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

  /*fun deleteEstablishmentById(id: String) {
    return establishmentRepository.deleteById(id)
  }*/

  fun convertEstablishmentToEstablishmentDto(establishment: Establishment): EstablishmentDto = EstablishmentDto(
    id = establishment.id,
    name = establishment.name,
  )

  fun convertEstablishmentDtoToEstablishment(establishmentDto: EstablishmentDto): Establishment = Establishment(
    id = establishmentDto.id,
    name = establishmentDto.name,
  )
}
