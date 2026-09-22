package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.ApplicationGroupResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.ApplicationTypeResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentApplicationTypeResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentApplicationTypeRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentRepository

@Service
class EstablishmentApplicationTypeServiceImpl(
  private val staffService: StaffService,
  private val establishmentService: EstablishmentService,
  private val prisonerService: PrisonerService,
  private val establishmentRepository: EstablishmentRepository,
  private val establishmentApplicationTypeRepository: EstablishmentApplicationTypeRepository,
) : EstablishmentApplicationTypeService {

  override fun getActiveApplicationTypesByStaffId(staffId: String): List<ApplicationGroupResponse> {
    val staff = staffService.getStaffById(staffId).orElseThrow {
      ApiException("No staff with id $staffId", HttpStatus.FORBIDDEN)
    }
    establishmentRepository.findById(staff.establishmentId).orElseThrow {
      ApiException("Establishment: ${staff.establishmentId} not enabled", HttpStatus.FORBIDDEN)
    }
    return getActiveApplicationTypesByEstablishmentId(staff.establishmentId)
  }

  override fun getActiveApplicationTypesByPrisonerId(prisonerId: String): List<ApplicationGroupResponse> {
    val prisoner = validatePrisoner(prisonerId)
    validateEstablishment(prisoner.establishmentId!!)
    return getActiveApplicationTypesByEstablishmentId(prisoner.establishmentId)
  }

  override fun getAllApplicationTypesByEstablishment(staffId: String): List<EstablishmentApplicationTypeResponse> {
    val staff = staffService.getStaffById(staffId).orElseThrow {
      ApiException("No staff with id $staffId", HttpStatus.FORBIDDEN)
    }
    establishmentRepository.findById(staff.establishmentId).orElseThrow {
      ApiException("Establishment: ${staff.establishmentId} not enabled", HttpStatus.FORBIDDEN)
    }

    return establishmentApplicationTypeRepository
      .findByEstablishmentId(
        staff.establishmentId,
        Sort.by(
          Sort.Order.asc("applicationType.applicationGroup.id"),
          Sort.Order.asc("applicationType.name"),
        ),
      )
      .mapNotNull { configured ->
        val type = configured.applicationType
        val group = type.applicationGroup ?: return@mapNotNull null
        EstablishmentApplicationTypeResponse(
          id = configured.id,
          establishmentId = staff.establishmentId,
          active = configured.active,
          applicationGroupResponse = ApplicationGroupResponse(
            id = group.id,
            name = group.name,
            appTypes = listOf(
              ApplicationTypeResponse(
                id = type.id,
                name = type.name,
                genericType = type.genericType,
                genericForm = type.genericForm,
                logDetailRequired = type.logDetailRequired,
                count = null,
              ),
            ),
          ),
        )
      }
  }

  private fun getActiveApplicationTypesByEstablishmentId(establishmentId: String): List<ApplicationGroupResponse> {
    val configuredAppTypes = establishmentApplicationTypeRepository
      .findByEstablishmentIdAndActive(
        establishmentId,
        true,
        Sort.by(Sort.Order.asc("applicationType.applicationGroup.id"), Sort.Order.asc("applicationType.name")),
      )

    val typesByGroup = configuredAppTypes
      .mapNotNull { configuredType -> configuredType.applicationType.applicationGroup?.let { it to configuredType.applicationType } }
      .groupBy({ it.first }, { it.second })

    return typesByGroup.map { (appGroup, appTypes) ->
      ApplicationGroupResponse(
        id = appGroup.id,
        name = appGroup.name,
        appTypes = appTypes.map { type ->
          ApplicationTypeResponse(
            id = type.id,
            name = type.name,
            genericType = type.genericType,
            genericForm = type.genericForm,
            logDetailRequired = type.logDetailRequired,
            count = null,
          )
        },
      )
    }
  }

  private fun validateEstablishment(establishmentId: String): EstablishmentDto = establishmentService.getEstablishmentById(establishmentId).orElseThrow {
    ApiException("Establishment with id $establishmentId not onboarded", HttpStatus.FORBIDDEN)
  }

  private fun validatePrisoner(prisonerId: String): Prisoner {
    val prisoner = prisonerService.getPrisonerById(prisonerId).orElseThrow {
      ApiException("Prison with id $prisonerId not found", HttpStatus.NOT_FOUND)
    }
    return prisoner
  }
}
