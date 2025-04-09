package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.CommentRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.CommentResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.PageResultComments
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.CommentService
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import java.util.*

@RestController
@RequestMapping("v1")
class CommentResource(val commentService: CommentService) {

  @PostMapping(
    "/prisoners/{prisonerId}/apps/{appId}/comments",
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  fun addComment(
    @PathVariable prisonerId: String,
    @PathVariable appId: UUID,
    @RequestBody commentRequestDto: CommentRequestDto,
    authentication: Authentication,
  ): ResponseEntity<CommentResponseDto<Any>> {
    authentication as AuthAwareAuthenticationToken
    val comment = commentService.addComment(prisonerId, authentication.principal, appId, commentRequestDto)
    return ResponseEntity.status(HttpStatus.CREATED).body(comment)
  }

  @GetMapping(
    "/prisoners/{prisonerId}/apps/{appId}/comments/{commentId}",
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  fun getCommentById(
    @PathVariable prisonerId: String,
    @PathVariable appId: UUID,
    @PathVariable commentId: UUID,
    @RequestParam(required = false) createdBy: Boolean,
    authentication: Authentication,
  ): ResponseEntity<CommentResponseDto<Any>> {
    authentication as AuthAwareAuthenticationToken
    val comment = commentService.getCommentById(prisonerId, authentication.principal, appId, createdBy, commentId)
    return ResponseEntity.status(HttpStatus.OK).body(comment)
  }

  @GetMapping(
    "/prisoners/{prisonerId}/apps/{appId}/comments",
  )
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS')")
  fun getCommentsByAppId(
    @PathVariable prisonerId: String,
    @PathVariable appId: UUID,
    @RequestParam(required = true) page: Long,
    @RequestParam(required = true) size: Long,
    @RequestParam(required = false) createdBy: Boolean,
    authentication: Authentication,
  ): ResponseEntity<PageResultComments> {
    authentication as AuthAwareAuthenticationToken
    val comments =
      commentService.getCommentsByAppId(prisonerId, authentication.principal, appId, createdBy, page, size)
    return ResponseEntity.status(HttpStatus.OK).body(comments)
  }
}
