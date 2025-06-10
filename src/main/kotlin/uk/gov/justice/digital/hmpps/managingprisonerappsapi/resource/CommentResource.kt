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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.CommentRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.CommentResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PageResultComments
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.service.CommentService
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.*

@RestController
@RequestMapping("v1")
class CommentResource(val commentService: CommentService) {

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Tag(name = "Comments")
  @Operation(
    summary = "Add a comment to an App request of a prisoner.",
    description = "This api endpoint is for adding comment to an app request of a prisoner. The logged staff and prisoner  for whom app request created should belongs to same establishment for adding comment. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "Comment added successfully"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. The issue can be logged staff and prisoner have different establishment.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping(
    "/prisoners/{prisonerId}/apps/{appId}/comments",
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS', 'PRISON')")
  fun addComment(
    @PathVariable prisonerId: String,
    @PathVariable appId: UUID,
    @RequestBody commentRequestDto: CommentRequestDto,
    authentication: Authentication,
  ): ResponseEntity<CommentResponseDto<Any>> {
    logger.info("Request received for adding comment for app: $appId")
    authentication as AuthAwareAuthenticationToken
    val comment = commentService.addComment(prisonerId, authentication.principal, appId, commentRequestDto)
    return ResponseEntity.status(HttpStatus.CREATED).body(comment)
  }

  @Tag(name = "Comments")
  @Operation(
    summary = "Get a comment by comment id",
    description = "This api endpoint is for getting comment details. The logged staff and prisoner should belongs to same establishment. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "Comment data returned in response successfully."),
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
  @GetMapping(
    "/prisoners/{prisonerId}/apps/{appId}/comments/{commentId}",
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS', 'PRISON')")
  fun getCommentById(
    @PathVariable prisonerId: String,
    @PathVariable appId: UUID,
    @PathVariable commentId: UUID,
    @RequestParam(required = false) createdBy: Boolean,
    authentication: Authentication,
  ): ResponseEntity<CommentResponseDto<Any>> {
    logger.info("Request received to  get comment for app: $appId and comment: $commentId")
    authentication as AuthAwareAuthenticationToken
    val comment = commentService.getCommentById(prisonerId, authentication.principal, appId, createdBy, commentId)
    return ResponseEntity.status(HttpStatus.OK).body(comment)
  }

  @Tag(name = "Comments")
  @Operation(
    summary = "Get all comments for a give app by app id",
    description = "This api endpoint is for getting list of comments by app Id. The logged staff and prisoner should belongs to same establishment. Requires role ROLE_MANAGING_PRISONER_APPS",
    security = [SecurityRequirement(name = "MANAGING_PRISONER_APPS")],
    responses = [
      ApiResponse(responseCode = "200", description = "List fo comments returned successfully."),
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
  @GetMapping(
    "/prisoners/{prisonerId}/apps/{appId}/comments",
  )
  @PreAuthorize("hasAnyRole('MANAGING_PRISONER_APPS', 'PRISON')")
  fun getCommentsByAppId(
    @PathVariable prisonerId: String,
    @PathVariable appId: UUID,
    @RequestParam(required = true) page: Long,
    @RequestParam(required = true) size: Long,
    @RequestParam(required = false) createdBy: Boolean,
    authentication: Authentication,
  ): ResponseEntity<PageResultComments> {
    logger.info("Request received to  get comments for app: $appId")
    authentication as AuthAwareAuthenticationToken
    val comments =
      commentService.getCommentsByAppId(prisonerId, authentication.principal, appId, createdBy, page, size)
    return ResponseEntity.status(HttpStatus.OK).body(comments)
  }
}
