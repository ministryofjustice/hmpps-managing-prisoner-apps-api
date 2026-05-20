package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import com.fasterxml.uuid.Generators
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.request.CommentRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.CommentResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PageResultComments
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrisonerDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.StaffDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.CommentVisibility
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class CommentServiceImpl(
  private val staffService: StaffService,
  @Qualifier("appServiceV2") private val appService: AppService,
  private val commentRepository: CommentRepository,
  private val establishmentService: EstablishmentService,
  private val activityService: ActivityService,
  private val prisonerService: PrisonerService,
) : CommentService {

  override fun saveComment(comment: Comment): Comment = commentRepository.save(comment)

  override fun addCommentByStaff(
    prisonerId: String,
    staffId: String,
    appId: UUID,
    commentRequestDto: CommentRequestDto,
  ): CommentResponseDto<Any> {
    val staff = getStaff(staffId)
    val app = getAppById(appId)
    if (app.status == AppStatus.APPROVED || app.status == AppStatus.DECLINED) {
      throw ApiException("Comment cannot be added as app is already closed", HttpStatus.FORBIDDEN)
    }
    validateStaffPermission(staff, app)
    validatePrisonerByRequestedBy(prisonerId, app)
    val comment = commentRepository.save(
      Comment(
        Generators.timeBasedEpochGenerator().generate(),
        commentRequestDto.message,
        LocalDateTime.now(ZoneOffset.UTC),
        staffId,
        appId,
        commentRequestDto.visibility,
        UserCategory.STAFF,
      ),
    )
    app.comments.add(comment.id)
    appService.saveApp(app)
    activityService.addActivity(
      comment.id,
      EntityType.COMMENT,
      app.id,
      Activity.COMMENT_ADDED,
      app.establishmentId,
      staffId,
      LocalDateTime.now(ZoneOffset.UTC),
      prisonerId,
      app.applicationType!!,
      app.applicationGroup!!,
    )
    return convertCommentToCommentResponseDto(prisonerId, staff.username, comment)
  }

  override fun addCommentByPrisoner(
    prisonerId: String,
    appId: UUID,
    commentRequestDto: CommentRequestDto,
  ): CommentResponseDto<Any> {
    val prisoner = validatePrisoner(prisonerId)
    val app = getAppById(appId)
    validatePrisonerByRequestedBy(prisonerId, app)
    val comment = commentRepository.save(
      Comment(
        Generators.timeBasedEpochGenerator().generate(),
        commentRequestDto.message,
        LocalDateTime.now(ZoneOffset.UTC),
        prisonerId,
        appId,
        CommentVisibility.STAFF_AND_PRISONER,
        UserCategory.PRISONER,
      ),
    )
    app.comments.add(comment.id)
    appService.saveApp(app)
    activityService.addActivity(
      comment.id,
      EntityType.COMMENT,
      app.id,
      Activity.COMMENT_ADDED,
      app.establishmentId,
      prisonerId,
      LocalDateTime.now(ZoneOffset.UTC),
      prisonerId,
      app.applicationType!!,
      app.applicationGroup!!,
    )
    return convertCommentToCommentResponseDto(prisonerId, prisoner.username, comment)
  }

  override fun getCommentByIdForStaff(
    prisonerId: String,
    staffId: String,
    appId: UUID,
    createdBy: Boolean,
    commentId: UUID,
  ): CommentResponseDto<Any> {
    val staff = getStaff(staffId)
    val app = getAppById(appId)
    validateStaffPermission(staff, app)
    validatePrisonerByRequestedBy(prisonerId, app)
    val comment = commentRepository.findById(commentId).orElseThrow {
      throw ApiException("Comment with id $commentId does not exist", HttpStatus.NOT_FOUND)
    }
    if (createdBy) {
      val establishment = establishmentService.getEstablishmentById(staff.establishmentId).orElseThrow {
        ApiException("Establishment of Staff who created comment do not exist ${staff.establishmentId}", HttpStatus.NOT_FOUND)
      }
      val staffDto = StaffDto(
        staff.username,
        staff.userId,
        "${staff.fullName}",
        UserCategory.STAFF,
        establishment,
      )
      return convertCommentToCommentResponseDto(prisonerId, staffDto, comment)
    } else {
      return convertCommentToCommentResponseDto(prisonerId, comment.createdBy, comment)
    }
  }

  override fun getCommentByIdForPrisoner(
    prisonerId: String,
    appId: UUID,
    createdBy: Boolean,
    commentId: UUID,
  ): CommentResponseDto<Any> {
    val app = getAppById(appId)
    val prisoner = validatePrisoner(prisonerId)
    validatePrisonerByRequestedBy(prisonerId, app)
    val comment = commentRepository.findById(commentId).orElseThrow {
      throw ApiException("Comment with id $commentId does not exist", HttpStatus.NOT_FOUND)
    }
    if (comment.visibility == CommentVisibility.STAFF_ONLY) {
      throw ApiException("Comment with id $commentId cannot be viewed by prisoner", HttpStatus.FORBIDDEN)
    }
    if (createdBy) {
      val establishment = establishmentService.getEstablishmentById(prisoner.establishmentId!!).orElseThrow {
        ApiException("Establishment of Staff who created comment do not exist ${prisoner.establishmentId}", HttpStatus.NOT_FOUND)
      }
      val prisonerDto = PrisonerDto(
        prisoner.username,
        prisoner.userId,
        "${prisoner.firstName} ${prisoner.lastName}",
        UserCategory.PRISONER,
        establishment.id,
      )

      return convertCommentToCommentResponseDto(prisonerId, prisonerDto, comment)
    } else {
      return convertCommentToCommentResponseDto(prisonerId, comment.createdBy, comment)
    }
  }

  override fun getCommentsByAppIdForStaff(
    prisonerId: String,
    staffId: String,
    appId: UUID,
    createdBy: Boolean,
    pageNumber: Long,
    pageSize: Long,
  ): PageResultComments {
    val staff = getStaff(staffId)
    val app = getAppById(appId)
    validateStaffPermission(staff, app)
    validatePrisonerByRequestedBy(prisonerId, app)
    val pageRequest = PageRequest.of(pageNumber.toInt() - 1, pageSize.toInt())
    val pageResult = commentRepository.getCommentsByAppId(appId, pageRequest)
    return PageResultComments(
      (pageResult.pageable.pageNumber + 1),
      pageResult.totalElements.toLong(),
      pageResult.isLast,
      convertCommentsToCommentResponseDtoList(prisonerId, createdBy, pageResult.content),
    )
  }

  override fun getCommentsByAppIdForPrisoner(
    prisonerId: String,
    appId: UUID,
    createdBy: Boolean,
    pageNumber: Long,
    pageSize: Long,
  ): PageResultComments {
    val app = getAppById(appId)
    validatePrisonerByRequestedBy(prisonerId, app)
    val pageRequest = PageRequest.of(pageNumber.toInt() - 1, pageSize.toInt())
    val pageResult = commentRepository.getCommentsByAppIdAndVisibility(appId, CommentVisibility.STAFF_AND_PRISONER, pageRequest)
    return PageResultComments(
      (pageResult.pageable.pageNumber + 1),
      pageResult.totalElements.toLong(),
      pageResult.isLast,
      convertCommentsToCommentResponseDtoList(prisonerId, createdBy, pageResult.content),
    )
  }

  private fun convertCommentToCommentResponseDto(
    prisonerId: String,
    staffOrPrisoner: Any,
    comment: Comment,
  ): CommentResponseDto<Any> = CommentResponseDto(
    comment.id,
    comment.appId,
    comment.message,
    prisonerId,
    comment.createdDate,
    staffOrPrisoner,
    comment.visibility,
    comment.createdByUserType,
  )

  private fun convertCommentsToCommentResponseDtoList(
    prisonerId: String,
    createdBy: Boolean,
    comments: List<Comment>,
  ): List<CommentResponseDto<Any>> {
    val list = ArrayList<CommentResponseDto<Any>>()
    comments.forEach { comment ->
      var createdByPerson: Any = comment.createdBy
      if (createdBy) {
        if (comment.createdByUserType == UserCategory.STAFF) {
          staffService.getStaffById(comment.createdBy).ifPresent { staff ->
            val establishment = establishmentService.getEstablishmentById(staff.establishmentId).orElseThrow {
              ApiException("Establishment not added for  id ${staff.establishmentId}", HttpStatus.BAD_REQUEST)
            }
            createdByPerson = StaffDto(
              staff.username,
              staff.userId,
              "${staff.fullName}",
              UserCategory.STAFF,
              establishment,
            )
          }
        } else if (comment.createdByUserType == UserCategory.PRISONER) {
          prisonerService.getPrisonerById(comment.createdBy).ifPresent { prisoner ->
            val establishment = establishmentService.getEstablishmentById(prisoner.establishmentId!!).orElseThrow {
              ApiException("Establishment not added for  id ${prisoner.establishmentId}", HttpStatus.BAD_REQUEST)
            }
            createdByPerson = PrisonerDto(
              prisoner.username,
              prisoner.userId,
              "${prisoner.firstName} ${prisoner.lastName}",
              UserCategory.PRISONER,
              establishment.id,
            )
          }
        }
      }
      list.add(
        CommentResponseDto(
          comment.id,
          comment.appId,
          comment.message,
          prisonerId,
          comment.createdDate,
          createdByPerson,
          comment.visibility,
          comment.createdByUserType,
        ),
      )
    }
    return list
  }

  private fun getAppById(appId: UUID): App = appService.getAppById(appId).orElseThrow {
    ApiException("App with id $appId does not exist", HttpStatus.FORBIDDEN)
  }

  private fun getStaff(staffId: String): Staff = staffService.getStaffById(staffId).orElseThrow {
    ApiException("Staff with id $staffId does not exist", HttpStatus.NOT_FOUND)
  }

  private fun validateStaffPermission(staff: Staff, app: App) {
    if (staff.establishmentId != app.establishmentId) {
      throw ApiException("Staff with id ${staff.username}do not have permission to view other establishment App", HttpStatus.FORBIDDEN)
    }
  }

  private fun validatePrisonerByRequestedBy(prisonerId: String, app: App) {
    if (prisonerId != app.requestedBy) {
      throw ApiException("App with id ${app.id} is not requested by $prisonerId", HttpStatus.FORBIDDEN)
    }
  }

  private fun validatePrisoner(prisonerId: String): Prisoner {
    val prisoner = prisonerService.getPrisonerById(prisonerId).orElseThrow {
      ApiException("Prison with id $prisonerId not found", HttpStatus.NOT_FOUND)
    }
    return prisoner
  }
}
