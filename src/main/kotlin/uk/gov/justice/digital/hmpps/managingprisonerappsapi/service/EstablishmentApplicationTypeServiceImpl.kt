package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.EstablishmentApplicationTypeRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.ApplicationGroupResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.ApplicationTypeResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentApplicationTypeResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EstablishmentApplicationType
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

  override fun getAllApplicationTypesByEstablishment(staffId: String): List<EstablishmentApplicationTypeResponseDto> {
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
      .mapNotNull { it.toResponseDtoOrNull() }
  }

  override fun saveEstablishmentApplicationTypes(staffId: String, establishmentApplicationTypeRequestDtoList: List<EstablishmentApplicationTypeRequestDto>): List<EstablishmentApplicationTypeResponseDto> {
    val staff = staffService.getStaffById(staffId).orElseThrow {
      ApiException("No staff with id", HttpStatus.FORBIDDEN)
    }
    val establishment = establishmentRepository.findById(staff.establishmentId).orElseThrow {
      ApiException("Establishment not enabled", HttpStatus.FORBIDDEN)
    }
    validateEstablishment(establishmentApplicationTypeRequestDtoList.first().establishmentId)

    if (establishment.id != establishmentApplicationTypeRequestDtoList.first().establishmentId) {
      throw ApiException("Staff does not belong to establishment", HttpStatus.FORBIDDEN)
    }

    var savedList: MutableList<EstablishmentApplicationTypeResponseDto> = mutableListOf()
    for (establishmentApplicationTypeRequestDto in establishmentApplicationTypeRequestDtoList) {
      val existing = establishmentApplicationTypeRepository.findById(establishmentApplicationTypeRequestDto.id)
        .orElseThrow {
          ApiException("Application type with id ${establishmentApplicationTypeRequestDto.id} not found", HttpStatus.NOT_FOUND)
        }

      val saved = establishmentApplicationTypeRepository.save(
        existing.copy(
          active = establishmentApplicationTypeRequestDto.active,
          departmentId = establishmentApplicationTypeRequestDto.departmentId,
          lastModifiedBy = staffId,
          lastModifiedDate = java.time.LocalDateTime.now(),
        ),
      )
      savedList.add(saved.toResponseDto())
    }
    return savedList
  }

  private fun EstablishmentApplicationType.toResponseDto(): EstablishmentApplicationTypeResponseDto = toResponseDtoOrNull()
    ?: error("ApplicationType ${applicationType.id} has no applicationGroup")

  private fun EstablishmentApplicationType.toResponseDtoOrNull(): EstablishmentApplicationTypeResponseDto? {
    val group = applicationType.applicationGroup ?: return null
    return EstablishmentApplicationTypeResponseDto(
      id = id!!,
      establishmentId = establishment.id,
      departmentId = departmentId,
      active = active,
      applicationGroupResponse = ApplicationGroupResponse(
        id = group.id,
        name = group.name,
        appTypes = listOf(
          applicationType.toResponseDto(
            count = null,
          ),
        ),
      ),
    )
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
          type.toResponseDto(
            count = null,
          )
        },
      )
    }
  }

  private fun ApplicationType.toResponseDto(count: Long? = null) = ApplicationTypeResponse(
    id = id,
    name = name,
    genericType = genericType,
    genericForm = genericForm,
    logDetailRequired = logDetailRequired,
    count = count,
  )

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
