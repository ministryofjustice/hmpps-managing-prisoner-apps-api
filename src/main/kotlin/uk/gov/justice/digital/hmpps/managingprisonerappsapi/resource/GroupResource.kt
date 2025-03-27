package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.AssignedGroupDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.GroupsRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.GroupService
import java.util.UUID

@RestController
@RequestMapping("v1")
class GroupResource(private val groupService: GroupService) {

  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  @PostMapping("/groups", produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
  fun createGroup(@RequestBody groupsRequestDto: GroupsRequestDto): ResponseEntity<AssignedGroupDto> {
    val createdGroup = groupService.createGroup(groupsRequestDto)
    return ResponseEntity.status(HttpStatus.CREATED).body(createdGroup)
  }

  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  @GetMapping("/groups/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getGroupById(@PathVariable id: UUID): ResponseEntity<AssignedGroupDto> {
    val group = groupService.getGroupById(id)
    return ResponseEntity.status(HttpStatus.OK).body(group)
  }

  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  @PutMapping(
    "/groups/{id}",
    produces = [MediaType.APPLICATION_JSON_VALUE],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun updateGroup(
    @PathVariable id: UUID,
    @RequestBody groupsRequestDto: GroupsRequestDto,
  ): ResponseEntity<AssignedGroupDto> {
    val updatedGroup = groupService.updateGroup(groupsRequestDto)
    return ResponseEntity.status(HttpStatus.OK).body(updatedGroup)
  }

  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  @DeleteMapping(
    "/groups/{id}",
    produces = [MediaType.APPLICATION_JSON_VALUE],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun deleteGroup(@PathVariable id: UUID): ResponseEntity<Void> {
    groupService.deleteGroupById(id)
    return ResponseEntity.status(HttpStatus.OK).build()
  }

  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  @GetMapping("/groups", produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getGroupsByEstablishment(): ResponseEntity<List<AssignedGroupDto>> {
    val groups = groupService.getGroupsByEstablishmentId("TEST_ESTABLISHMENT_FIRST")
    return ResponseEntity.status(HttpStatus.OK).body(groups)
  }
}
