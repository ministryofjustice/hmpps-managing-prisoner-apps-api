package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.EstablishmentApplicationTypeRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.EstablishmentDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationGroup
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.ApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EstablishmentApplicationType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentApplicationTypeRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentRepository
import java.util.Optional
import java.util.UUID

class EstablishmentApplicationTypeServiceImplTest {

  private lateinit var staffService: StaffService
  private lateinit var establishmentService: EstablishmentService
  private lateinit var prisonerService: PrisonerService
  private lateinit var establishmentRepository: EstablishmentRepository
  private lateinit var establishmentApplicationTypeRepository: EstablishmentApplicationTypeRepository
  private lateinit var service: EstablishmentApplicationTypeServiceImpl

  private val establishmentId = "RNI"
  private val staffId = "STAFF_1"
  private val prisonerId = "A1234BC"

  @BeforeEach
  fun setUp() {
    staffService = Mockito.mock(StaffService::class.java)
    establishmentService = Mockito.mock(EstablishmentService::class.java)
    prisonerService = Mockito.mock(PrisonerService::class.java)
    establishmentRepository = Mockito.mock(EstablishmentRepository::class.java)
    establishmentApplicationTypeRepository = Mockito.mock(EstablishmentApplicationTypeRepository::class.java)

    service = EstablishmentApplicationTypeServiceImpl(
      staffService,
      establishmentService,
      prisonerService,
      establishmentRepository,
      establishmentApplicationTypeRepository,
    )
  }

  @Test
  fun `getActiveApplicationTypesByStaffId returns grouped active application types`() {
    val staff = Staff(staffId, "user-id", "Test Staff", UserCategory.STAFF, establishmentId, "Officer", UUID.randomUUID())
    val establishment = Establishment(establishmentId, "Test", false, emptySet(), emptySet())
    val appGroup = ApplicationGroup(10L, "PIN")
    val appTypeOne = ApplicationType(1L, "Supply list of contacts", false, false, false, appGroup)
    val appTypeTwo = ApplicationType(2L, "Remove a PIN phone contact", true, false, true, appGroup)

    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))
    Mockito.`when`(establishmentRepository.findById(establishmentId)).thenReturn(Optional.of(establishment))
    Mockito.`when`(
      establishmentApplicationTypeRepository.findByEstablishmentIdAndActive(eq(establishmentId), eq(true), any()),
    ).thenReturn(
      listOf(
        EstablishmentApplicationType(establishment = establishment, applicationType = appTypeOne, active = true),
        EstablishmentApplicationType(establishment = establishment, applicationType = appTypeTwo, active = true),
      ),
    )

    val response = service.getActiveApplicationTypesByStaffId(staffId)

    assertEquals(1, response.size)
    assertEquals(10L, response[0].id)
    assertEquals("PIN", response[0].name)
    assertEquals(listOf(1L, 2L), response[0].appTypes?.map { it.id })
  }

  @Test
  fun `getAllApplicationTypesByEstablishment returns mapped rows and skips types without group`() {
    val staff = Staff(staffId, "user-id", "Test Staff", UserCategory.STAFF, establishmentId, "Officer", UUID.randomUUID())
    val establishment = Establishment(establishmentId, "Test", false, emptySet(), emptySet())
    val appGroup = ApplicationGroup(30L, "Gym")
    val groupedType = ApplicationType(11L, "Gym booking", false, false, false, appGroup)
    val noGroupType = ApplicationType(12L, "Gym new starter", false, false, false, null)

    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))
    Mockito.`when`(establishmentRepository.findById(establishmentId)).thenReturn(Optional.of(establishment))
    Mockito.`when`(establishmentApplicationTypeRepository.findByEstablishmentId(eq(establishmentId), any())).thenReturn(
      listOf(
        EstablishmentApplicationType(id = UUID.randomUUID(), establishment = establishment, applicationType = groupedType, active = true),
        EstablishmentApplicationType(id = UUID.randomUUID(), establishment = establishment, applicationType = noGroupType, active = true),
      ),
    )

    val response = service.getAllApplicationTypesByEstablishment(staffId)

    assertEquals(1, response.size)
    assertEquals(establishmentId, response[0].establishmentId)
    assertEquals(30L, response[0].applicationGroupResponse.id)
    assertEquals(listOf(11L), response[0].applicationGroupResponse.appTypes?.map { it.id })
  }

  @Test
  fun `saveEstablishmentApplicationTypes updates active and department fields`() {
    val mappingId = UUID.randomUUID()
    val departmentId = UUID.randomUUID()
    val staff = Staff(staffId, "user-id", "Test Staff", UserCategory.STAFF, establishmentId, "Officer", UUID.randomUUID())
    val establishment = Establishment(establishmentId, "Test", false, emptySet(), emptySet())
    val appGroup = ApplicationGroup(99L, "Group")
    val appType = ApplicationType(9L, "Type", false, false, false, appGroup)
    val existing = EstablishmentApplicationType(
      id = mappingId,
      establishment = establishment,
      applicationType = appType,
      active = true,
      departmentId = null,
    )

    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))
    Mockito.`when`(establishmentRepository.findById(establishmentId)).thenReturn(Optional.of(establishment))
    Mockito.`when`(establishmentService.getEstablishmentById(establishmentId))
      .thenReturn(Optional.of(EstablishmentDto(establishmentId, "Test", false, emptySet(), emptySet())))
    Mockito.`when`(establishmentApplicationTypeRepository.findById(mappingId)).thenReturn(Optional.of(existing))
    Mockito.`when`(establishmentApplicationTypeRepository.save(any())).thenAnswer { it.arguments[0] as EstablishmentApplicationType }

    val response = service.saveEstablishmentApplicationTypes(
      staffId,
      listOf(
        EstablishmentApplicationTypeRequestDto(
          id = mappingId,
          applicationTypeId = 9L,
          establishmentId = establishmentId,
          departmentId = departmentId,
          active = false,
        ),
      ),
    )

    assertEquals(1, response.size)
    assertEquals(mappingId, response[0].id)
    assertEquals(false, response[0].active)
    assertEquals(departmentId, response[0].departmentId)
    assertNotNull(response[0].applicationGroupResponse.appTypes)
  }

  @Test
  fun `saveEstablishmentApplicationTypes throws forbidden when staff establishment mismatches request`() {
    val mappingId = UUID.randomUUID()
    val staff = Staff(staffId, "user-id", "Test Staff", UserCategory.STAFF, establishmentId, "Officer", UUID.randomUUID())
    val otherEstablishmentId = "WLI"
    val establishment = Establishment(establishmentId, "Test", false, emptySet(), emptySet())

    Mockito.`when`(staffService.getStaffById(staffId)).thenReturn(Optional.of(staff))
    Mockito.`when`(establishmentRepository.findById(establishmentId)).thenReturn(Optional.of(establishment))
    Mockito.`when`(establishmentService.getEstablishmentById(otherEstablishmentId))
      .thenReturn(Optional.of(EstablishmentDto(otherEstablishmentId, "Other", false, emptySet(), emptySet())))

    val exception = assertThrows(ApiException::class.java) {
      service.saveEstablishmentApplicationTypes(
        staffId,
        listOf(
          EstablishmentApplicationTypeRequestDto(
            id = mappingId,
            applicationTypeId = 9L,
            establishmentId = otherEstablishmentId,
            departmentId = null,
            active = true,
          ),
        ),
      )
    }

    assertEquals(HttpStatus.FORBIDDEN, exception.status)
    assertEquals("Staff does not belong to establishment", exception.message)
  }

  @Test
  fun `getActiveApplicationTypesByPrisonerId throws not found when prisoner is missing`() {
    Mockito.`when`(prisonerService.getPrisonerById(prisonerId)).thenReturn(Optional.empty())

    val exception = assertThrows(ApiException::class.java) {
      service.getActiveApplicationTypesByPrisonerId(prisonerId)
    }

    assertEquals(HttpStatus.NOT_FOUND, exception.status)
    assertEquals("Prison with id $prisonerId not found", exception.message)
  }
}
