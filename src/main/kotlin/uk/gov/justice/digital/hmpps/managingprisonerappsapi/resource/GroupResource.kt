package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
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

  @PostMapping("/groups", path = [MediaType.APPLICATION_JSON_VALUE], consumes =  [MediaType.APPLICATION_JSON_VALUE])
  fun createGroup(@RequestBody groupsRequestDto: GroupsRequestDto): ResponseEntity<AssignedGroupDto> {
    val createdGroup = groupService.createGroup(groupsRequestDto)
    return ResponseEntity.status(HttpStatus.CREATED).body(createdGroup)
  }
  @GetMapping("/groups/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getGroupById(@PathVariable id: UUID): ResponseEntity<AssignedGroupDto> {
    val group = groupService.getGroupById(id)
    return ResponseEntity.status(HttpStatus.OK).body(group)
  }

  @PutMapping("/groups/{id}", produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
  fun updateGroup(@PathVariable id: UUID, @RequestBody groupsRequestDto: GroupsRequestDto): ResponseEntity<AssignedGroupDto>  {
    val updatedGroup = groupService.updateGroup(groupsRequestDto)
    return ResponseEntity.status(HttpStatus.OK).body(updatedGroup)
  }

  @DeleteMapping("/groups/{id}", produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
  fun deleteGroup(@PathVariable id: UUID):ResponseEntity<Void> {
    groupService.deleteGroupById(id)
    return ResponseEntity.status(HttpStatus.OK).build()
  }

  @GetMapping("groups/{establishmentId}", produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getGroupsByEstablishment(@PathVariable establishmentId: String):ResponseEntity<List<AssignedGroupDto>>  {
    val groups = groupService.getGroupsByEstablishmentId(establishmentId)
    return ResponseEntity.status(HttpStatus.OK).body(groups)
  }
}