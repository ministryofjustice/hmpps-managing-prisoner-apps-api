package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AppTypeResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentRepository
import java.util.*

@Service
class EstablishmentService(
  private val establishmentRepository: EstablishmentRepository,
  private val staffService: StaffService,
) {

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

  fun getAppTypesByLoggedUserEstablishment(staffId: String): List<AppTypeResponse> {
    val staff = staffService.getStaffById(staffId).orElseThrow {
      ApiException("No staff with id $staffId", HttpStatus.FORBIDDEN)
    }
    val establishment = establishmentRepository.findById(staff.establishmentId).orElseThrow {
      ApiException("Establishment: ${staff.establishmentId} not enabled", HttpStatus.FORBIDDEN)
    }
    return convertAppTypeToAppTypeListResponse(establishment.appTypes)
  }

  private fun convertEstablishmentToEstablishmentDto(establishment: Establishment): EstablishmentDto = EstablishmentDto(
    id = establishment.id,
    name = establishment.name,
    establishment.appTypes,
  )

  private fun convertEstablishmentDtoToEstablishment(establishmentDto: EstablishmentDto): Establishment = Establishment(
    id = establishmentDto.id,
    name = establishmentDto.name,
    establishmentDto.appTypes,
  )

  private fun convertAppTypeToAppTypeListResponse(appTypes: Set<AppType>): List<AppTypeResponse> {
    val appTypesList = ArrayList<AppTypeResponse>(appTypes.size)
    appTypes.forEach { appType ->
      if (appType == AppType.PIN_PHONE_ADD_NEW_SOCIAL_CONTACT) {
        appTypesList.add(
          AppTypeResponse(
            appType.name,
            "add-social-pin-phone-contact",
            "Add new social PIN phone contact",
          ),
        )
      } else if (appType == AppType.PIN_PHONE_ADD_NEW_LEGAL_CONTACT) {
        appTypesList.add(
          AppTypeResponse(
            appType.name,
            "add-legal-pin-phone-contact",
            "Add new legal PIN phone contact",
          ),
        )
      } else if (appType == AppType.PIN_PHONE_REMOVE_CONTACT) {
        appTypesList.add(
          AppTypeResponse(
            appType.name,
            "remove-pin-phone-contact",
            "Remove PIN phone contact",
          ),
        )
      } else if (appType == AppType.PIN_PHONE_EMERGENCY_CREDIT_TOP_UP) {
        appTypesList.add(
          AppTypeResponse(
            appType.name,
            "add-emergency-pin-phone-credit",
            "Add emergency PIN phone credit",
          ),
        )
      } else if (appType == AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS) {
        appTypesList.add(
          AppTypeResponse(
            appType.name,
            "swap-visiting-orders-for-pin-credit",
            "Swap visiting orders (VOs) for PIN credit",
          ),
        )
      } else if (appType == AppType.PIN_PHONE_SUPPLY_LIST_OF_CONTACTS) {
        appTypesList.add(
          AppTypeResponse(
            appType.name,
            "supply-list-of-pin-phone-contacts",
            "Supply list of PIN phone contacts",
          ),
        )
      } else {
        throw ApiException("No app type found for ${appType.name}", HttpStatus.INTERNAL_SERVER_ERROR)
      }
    }
    return appTypesList
  }
}
