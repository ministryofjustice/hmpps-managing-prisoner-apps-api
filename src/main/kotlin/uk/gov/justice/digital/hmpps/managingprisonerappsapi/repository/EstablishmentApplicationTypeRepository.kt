package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EstablishmentApplicationGroup

@Repository
interface EstablishmentApplicationTypeRepository :JpaRepository<EstablishmentApplicationGroup, String>

/*
Use Case
  findByIdEstablishmentId() Get all groups (active + inactive) for admin history view
  findByIdEstablishmentIdAndActiveTrue() only active groups for app creation screens
  findByIdEstablishmentIdAndActiveTrueOrderByDisplayOrder() Get active groups sorted by display order
  findByIdApplicationGroupId() Find all establishments using a specific group
  findByIdApplicationGroupIdAndActiveTrue() Find active establishments using a specific group
  countByIdApplicationGroupIdAndActiveTrue() Count how many establishments actively use a group
Note: The composite key naming convention requires id.establishmentId and id.applicationGroupId in method names
*/
