package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import com.fasterxml.uuid.Generators
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.ActivityMessage
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.HistoryResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.CommentVisibility
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.History
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.StaffType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.HistoryRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ResponseRepository
import java.time.LocalDateTime
import java.util.*

@Service
class HistoryServiceImpl(
  private val historyRepository: HistoryRepository,
  private val staffService: StaffService,
  private val groupService: GroupService,
  private val appRepository: AppRepository,
  private val commentRepository: CommentRepository,
  private val responseRepository: ResponseRepository,
) : HistoryService {
  companion object {
    val logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun getHistoryByAppId(appId: UUID, establishment: String): List<HistoryResponse> {
    val history = historyRepository.findByAppIdAndEstablishmentOrderByCreatedDate(appId, establishment)
    return convertHistoryEntityToHistory(appId, history)
  }

  override fun updateActivityInHistory(
    entityId: UUID,
    entityType: EntityType,
    appId: UUID,
    activity: Activity,
    establishment: String,
    createdBy: String,
    createdDate: LocalDateTime,
    reference: String?,
  ) {
    logger.info("Updating history for entity:$entityType, activity: $activity,  entityType: $entityType, appId: $appId")
    val history = historyRepository.save(
      History(
        Generators.timeBasedEpochGenerator().generate(),
        entityId,
        entityType,
        appId,
        activity,
        establishment,
        createdBy,
        createdDate,
        reference,
      ),
    )
  }

  private fun convertHistoryEntityToHistory(appId: UUID, history: List<History>): List<HistoryResponse> {
    val map = mutableMapOf<String, HistoryResponse>()

    val app = appRepository.findById(appId)
      .orElseThrow { ApiException("App with id $appId does not exist", HttpStatus.NOT_FOUND) }

    history.forEach { h ->
      val createdBy: String = when {
        h.createdBy == StaffType.MANAGE_APPS_ADMIN.name -> StaffType.MANAGE_APPS_ADMIN.name
        h.createdBy != app.requestedBy -> staffService.getStaffById(h.createdBy)
          .orElseThrow { ApiException("Staff with id ${h.createdBy} does not exist", HttpStatus.NOT_FOUND) }
          .fullName
        else -> "${app.requestedByFirstName} ${app.requestedByLastName} [PRISONER]"
      }

      val key = "${h.id}_${h.activity}_${h.createdBy}_${h.createdDate}"

      fun message(header: String, body: String? = null) = ActivityMessage(header, createdBy, body)
      fun toResponse(msg: ActivityMessage) = HistoryResponse(h.id, h.appId, h.entityId, h.entityType, msg, h.createdDate)

      val response: HistoryResponse? = when (h.activity) {
        Activity.APP_SUBMITTED -> {
          val groupName = groupService.getGroupById(h.entityId).name
          toResponse(message("Application logged", "Assigned to $groupName"))
        }
        Activity.APP_IN_PROGRESS -> toResponse(message("Application set to In Progress"))
        Activity.PRISONER_ID_UPDATE -> toResponse(message("Prisoner Id merged with ${h.reference}"))
        Activity.APP_REQUEST_FORM_DATA_UPDATED -> toResponse(message("Form data updated"))
        Activity.APP_FORWARDED_TO_A_GROUP -> {
          val groupName = groupService.getGroupById(h.entityId).name
          toResponse(message("Application forwarded", "Forwarded to $groupName"))
        }
        Activity.FORWARDING_COMMENT_ADDED, Activity.COMMENT_ADDED -> {
          if (h.entityType != EntityType.COMMENT) return@forEach
          commentRepository.findById(h.entityId).orElse(null)?.let { comment ->
            val header = when {
              h.activity == Activity.FORWARDING_COMMENT_ADDED -> "Comment"
              comment.visibility == CommentVisibility.STAFF_ONLY -> "Comment"
              comment.createdByUserType == UserCategory.PRISONER -> "Message from prisoner"
              else -> "Message to prisoner"
            }
            toResponse(message(header, comment.message))
          }
        }
        Activity.APP_APPROVED, Activity.APP_DECLINED, Activity.APP_REJECTED -> {
          if (h.entityType != EntityType.RESPONSE) return@forEach
          val reason = responseRepository.findById(h.entityId).orElse(null)?.reason ?: ""
          val prefix = when (h.activity) {
            Activity.APP_APPROVED -> "Application approved."
            Activity.APP_DECLINED -> "Application declined."
            else -> "Application rejected."
          }
          toResponse(message("Application closed.", "$prefix $reason"))
        }
        else -> null
      }
      response?.let { map[key] = it }
    }
    return map.values.toList()
  }
}
