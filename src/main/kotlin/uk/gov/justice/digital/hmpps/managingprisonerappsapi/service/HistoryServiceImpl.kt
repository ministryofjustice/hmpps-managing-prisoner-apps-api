package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.ActivityMessage
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.HistoryResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.History
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.HistoryRepository
import java.time.LocalDateTime
import java.util.*

@Service
class HistoryServiceImpl(
  private val historyRepository: HistoryRepository,
  private val staffService: StaffService,
  private val groupService: GroupService,
  private val commentRepository: CommentRepository,
) : HistoryService {
  companion object {
    val logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun getHistoryByAppId(appId: UUID, establishment: String): List<HistoryResponse> {
    /*val staff = staffService.getStaffById(user).orElseThrow {
      ApiException("Staff with id $user does not exist", HttpStatus.FORBIDDEN)
    }*/
    val history = historyRepository.findByAppIdAndEstablishmentOrderByCreatedDate(appId, establishment)
    return convertHistoryEntityToHistory(history)
  }

  override fun updateActivityInHistory(entityId: UUID, entityType: EntityType, appId: UUID, activity: Activity, establishment: String, createdBy: String, createdDate: LocalDateTime) {
    logger.info("Updating history for entity:$entityType, activity: $activity,  entityType: $entityType, appId: $appId")
    val history = historyRepository.save(
      History(
        UUID.randomUUID(),
        entityId,
        entityType,
        appId,
        activity,
        establishment,
        createdBy,
        createdDate,
      ),
    )
    println("history = $history")
    println("history list = ${historyRepository.findByAppIdAndEstablishmentOrderByCreatedDate(appId, establishment)}")
  }

  private fun convertHistoryEntityToHistory1(history: List<History>): List<HistoryResponse> {
    val list = mutableListOf<HistoryResponse>()
    history.forEach { h ->
      val createdBy = staffService.getStaffById(h.createdBy).orElseThrow {
        ApiException("Staff with id ${h.createdBy} does not exist", HttpStatus.NOT_FOUND)
      }
      var groupName: String = ""
      if (h.activity == Activity.APP_FORWARDED_TO_A_GROUP) {
        groupName = groupService.getGroupById(h.entityId).name
      }

      val historyResponse = HistoryResponse(
        h.id,
        h.appId,
        h.entityId,
        h.entityType,
        ActivityMessage(
          if (h.activity == Activity.APP_FORWARDED_TO_A_GROUP) {
            convertActivityToReadableMessage(h.activity, "${createdBy.fullName} to group $groupName")
          } else {
            convertActivityToReadableMessage(h.activity, createdBy.fullName)
          },
          null,
        ),
        h.createdDate,
      )
      list.add(historyResponse)
    }
    return list
  }

  private fun convertHistoryEntityToHistory(history: List<History>): List<HistoryResponse> {
    val map = mutableMapOf<String, HistoryResponse>()
    history.forEach { h ->
      val createdBy = staffService.getStaffById(h.createdBy).orElseThrow {
        ApiException("Staff with id ${h.createdBy} does not exist", HttpStatus.NOT_FOUND)
      }
      var groupName: String = ""
      if (h.activity != Activity.FORWARDING_COMMENT_ADDED && h.activity != Activity.APP_FORWARDED_TO_A_GROUP && h.activity != Activity.APP_SUBMITTED) {
        map.put(
          "${h.id}_${h.activity}_${h.createdBy}_${h.createdDate}",
          HistoryResponse(
            h.id,
            h.appId,
            h.entityId,
            h.entityType,
            ActivityMessage(
              if (h.activity == Activity.APP_FORWARDED_TO_A_GROUP) {
                convertActivityToReadableMessage(h.activity, "${createdBy.fullName} to group $groupName")
              } else {
                convertActivityToReadableMessage(h.activity, createdBy.fullName)
              },
              null,
            ),
            h.createdDate,
          ),
        )
      }
      if (h.activity == Activity.APP_SUBMITTED) {
        groupName = groupService.getGroupById(h.entityId).name
        map.put(
          "${h.id}_${h.activity}_${h.createdBy}_${h.createdDate}",
          HistoryResponse(
            h.id,
            h.appId,
            h.entityId,
            h.entityType,
            ActivityMessage("Logged by ${createdBy.fullName}", "Assigned to $groupName"),
            h.createdDate,
          ),
        )
      }
      if (h.activity == Activity.APP_FORWARDED_TO_A_GROUP) {
        groupName = groupService.getGroupById(h.entityId).name
        map.put(
          "${h.id}_${h.activity}_${h.createdBy}_${h.createdDate}",
          HistoryResponse(
            h.id,
            h.appId,
            h.entityId,
            h.entityType,
            ActivityMessage(
              "Forwarded to group $groupName by ${createdBy.fullName}",
              null,
            ),
            h.createdDate,
          ),
        )
      }
      if (h.activity == Activity.FORWARDING_COMMENT_ADDED && h.entityType == EntityType.COMMENT) {
        map.keys.forEach { k ->
          if (k.contains("${Activity.APP_FORWARDED_TO_A_GROUP}_${h.createdBy}_${h.createdDate}")) {
            var value = map[k]
            var activityMessage = value!!.activityMessage
            val comment = commentRepository.findById(h.entityId)
            if (comment.isPresent) {
              val message = ActivityMessage(activityMessage.header, comment.get().message)
              value.activityMessage = message
              map[k] = value
            }
          }
        }
      }
    }
    return map.values.toList()
  }

  private fun convertActivityToReadableMessage(activity: Activity, staffName: String): String {
    val x: String
    when (activity) {
      Activity.APP_SUBMITTED -> x = "Logged by $staffName"
      Activity.APP_REQUEST_FORM_DATA_UPDATED -> x = "Form data updated by $staffName"
      Activity.COMMENT_ADDED -> x = "Comment added by $staffName"
      Activity.FORWARDING_COMMENT_ADDED, Activity.APP_FORWARDED_TO_A_GROUP -> x = "Forwarding comment added by $staffName"
      Activity.APP_APPROVED -> x = "Marked as approved by $staffName"
      Activity.APP_DECLINED -> x = "Marked as declined by $staffName"
      else -> throw ApiException("Unknown activity type: $activity", HttpStatus.INTERNAL_SERVER_ERROR)
    }
    return x
  }
}
