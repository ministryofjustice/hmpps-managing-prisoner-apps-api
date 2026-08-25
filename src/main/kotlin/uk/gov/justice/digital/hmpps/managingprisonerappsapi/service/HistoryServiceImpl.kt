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
      var createdBy: String
      if (h.createdBy == StaffType.MANAGE_APPS_ADMIN.name) {
        createdBy = StaffType.MANAGE_APPS_ADMIN.name
      } else {
        if (h.createdBy != app.requestedBy) {
          // history data added due to staff action
          val staff = staffService.getStaffById(h.createdBy).orElseThrow {
            ApiException("Staff with id ${h.createdBy} does not exist", HttpStatus.NOT_FOUND)
          }
          createdBy = staff.fullName
        } else {
          // history data added due to prisoner action
          createdBy = "${app.requestedByFirstName} ${app.requestedByLastName} [PRISONER]"
        }
      }
      var groupName: String = ""

      if (h.activity == Activity.APP_SUBMITTED) {
        groupName = groupService.getGroupById(h.entityId).name
        map["${h.id}_${h.activity}_${h.createdBy}_${h.createdDate}"] = HistoryResponse(
          h.id,
          h.appId,
          h.entityId,
          h.entityType,
          ActivityMessage("Application logged", createdBy, "Assigned to $groupName"),
          h.createdDate,
        )
      } else if (h.activity == Activity.APP_IN_PROGRESS) {
        map["${h.id}_${h.activity}_${h.createdBy}_${h.createdDate}"] = HistoryResponse(
          h.id,
          h.appId,
          h.entityId,
          h.entityType,
          ActivityMessage("Application set to In Progress", createdBy, null),
          h.createdDate,
        )
      } else if (h.activity == Activity.PRISONER_ID_UPDATE) {
        map["${h.id}_${h.activity}_${h.createdBy}_${h.createdDate}"] = HistoryResponse(
          h.id,
          h.appId,
          h.entityId,
          h.entityType,
          ActivityMessage("Prisoner ID merged with ${h.reference}", createdBy, null),
          h.createdDate,
        )
      } else if (h.activity == Activity.APP_REQUEST_FORM_DATA_UPDATED) {
        map["${h.id}_${h.activity}_${h.createdBy}_${h.createdDate}"] = HistoryResponse(
          h.id,
          h.appId,
          h.entityId,
          h.entityType,
          ActivityMessage("Form data updated", createdBy, null),
          h.createdDate,
        )
      } else if (h.activity == Activity.APP_FORWARDED_TO_A_GROUP) {
        groupName = groupService.getGroupById(h.entityId).name
        map["${h.id}_${h.activity}_${h.createdBy}_${h.createdDate}"] = HistoryResponse(
          h.id,
          h.appId,
          h.entityId,
          h.entityType,
          ActivityMessage(
            "Application forwarded",
            createdBy,
            "Forwarded to $groupName",
          ),
          h.createdDate,
        )
      } else if (h.activity == Activity.FORWARDING_COMMENT_ADDED && h.entityType == EntityType.COMMENT) {
        val comment = commentRepository.findById(h.entityId)
        if (comment.isPresent) {
          map["${h.id}_${h.activity}_${h.createdBy}_${h.createdDate}"] = HistoryResponse(
            h.id,
            h.appId,
            h.entityId,
            h.entityType,
            ActivityMessage(
              "Comment",
              createdBy,
              comment.get().message,
            ),
            h.createdDate,
          )
        }
      } else if (h.activity == Activity.COMMENT_ADDED && h.entityType == EntityType.COMMENT) {
        val comment = commentRepository.findById(h.entityId)
        if (comment.isPresent) {
          var messageHeader: String = ""
          if (comment.get().visibility == CommentVisibility.STAFF_ONLY) {
            messageHeader = "Comment"
          } else if (comment.get().visibility == CommentVisibility.STAFF_AND_PRISONER) {
            if (comment.get().createdByUserType == UserCategory.PRISONER) {
              messageHeader = "Message from prisoner"
            } else {
              messageHeader = "Message to prisoner"
            }
          }
          map["${h.id}_${h.activity}_${h.createdBy}_${h.createdDate}"] = HistoryResponse(
            h.id,
            h.appId,
            h.entityId,
            h.entityType,
            ActivityMessage(
              messageHeader,
              createdBy,
              comment.get().message,
            ),
            h.createdDate,
          )
        }
      } else if (h.entityType == EntityType.RESPONSE) {
        var response = responseRepository.findById(h.entityId)
        var messageHeader: String = "Application closed."
        var messageBody: String = ""
        if (response.isPresent) {
          messageBody = response.get().reason
        }
        if (h.activity == Activity.APP_APPROVED) {
          messageBody = "Application approved. $messageBody"
        } else if (h.activity == Activity.APP_DECLINED) {
          messageBody = "Application declined. $messageBody"
        } else if (h.activity == Activity.APP_REJECTED) {
          messageBody = "Application rejected. $messageBody"
        }

        map["${h.id}_${h.activity}_${h.createdBy}_${h.createdDate}"] = HistoryResponse(
          h.id,
          h.appId,
          h.entityId,
          h.entityType,
          ActivityMessage(
            messageHeader,
            createdBy,
            messageBody,
          ),
          h.createdDate,
        )
      }
    }
    return map.values.toList()
  }
}
