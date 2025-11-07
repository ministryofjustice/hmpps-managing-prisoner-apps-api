package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AssignedGroupDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.GroupService
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.*

@RestController
@RequestMapping("v1")
class GroupResource(private val groupService: GroupService) {

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  /* @Tag(name = "Groups")
  @Operation(
    summary = "Add a group",
    description = "This api endpoint is for adding a group. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "Establishment add successfully"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  @PostMapping("/groups", produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
  fun createGroup(@RequestBody groupsRequestDto: GroupsRequestDto): ResponseEntity<AssignedGroupDto> {
    val createdGroup = groupService.createGroup(groupsRequestDto)
    return ResponseEntity.status(HttpStatus.CREATED).body(createdGroup)
  }*/

  @Tag(name = "Groups")
  @Operation(
    summary = "Get a group by id.",
    description = "This api endpoint is for getting a group by id. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "Establishment add successfully"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS', 'PRISON')")
  @GetMapping("/groups/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getGroupById(@PathVariable id: UUID): ResponseEntity<AssignedGroupDto> {
    val group = groupService.getGroupById(id, null)
    return ResponseEntity.status(HttpStatus.OK).body(group)
  }

  /*@Tag(name = "Groups")
  @Operation(
    summary = "Update a group by id",
    description = "This api endpoint is for updating a group. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "Establishment add successfully"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
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
  }*/

  /*@PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  @DeleteMapping(
    "/groups/{id}",
    produces = [MediaType.APPLICATION_JSON_VALUE],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun deleteGroup(@PathVariable id: UUID): ResponseEntity<Void> {
    groupService.deleteGroupById(id)
    return ResponseEntity.status(HttpStatus.OK).build()
  }*/

  @Tag(name = "Groups")
  @Operation(
    summary = "Get list of groups by logged user active caseload id.",
    description = "This api endpoint is for getting list of all groups for an establishment where establishment is logged user active case load id. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "Establishment add successfully"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS', 'PRISON')")
  @GetMapping("/groups", produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getGroupsByLoggedUserEstablishment(
    authentication: Authentication,
  ): ResponseEntity<List<AssignedGroupDto>> {
    authentication as AuthAwareAuthenticationToken
    logger.info("Request received to get groups by ${authentication.principal}")
    val groups = groupService.getGroupsByLoggedStaffEstablishmentId(authentication.principal)
    return ResponseEntity.status(HttpStatus.OK).body(groups)
  }

  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS', 'PRISON')")
  @GetMapping("/groups/app/types/{type}", produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getGroupsByAppType(
    authentication: Authentication,
    @PathVariable("type") type: Long,
  ): ResponseEntity<List<AssignedGroupDto>> {
    authentication as AuthAwareAuthenticationToken
    logger.info("Request received to get groups by ${authentication.principal}")
    val groups = groupService.getGroupsByLoggedStaffEstablishmentIdAndAppType(authentication.principal, type)
    return ResponseEntity.status(HttpStatus.OK).body(groups)
  }
}
