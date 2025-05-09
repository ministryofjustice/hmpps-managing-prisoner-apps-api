package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.HistoryResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.EntityType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.History
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.HistoryRepository
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class HistoryServiceImpl(
  private val historyRepository: HistoryRepository,
  private val staffService: StaffService,
  private val groupService: GroupService,
) : HistoryService {
  companion object {
    val logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun getHistoryByAppId(appId: UUID, establishment: String): List<HistoryResponse> {
    /*val staff = staffService.getStaffById(user).orElseThrow {
      ApiException("Staff with id $user does not exist", HttpStatus.FORBIDDEN)
    }*/
    val history = historyRepository.findByAppIdAndEstablishment(appId, establishment)
    return convertHistoryEntityToHistory(history)
  }

  override fun updateActivityInHistory(entityId: UUID, entityType: EntityType, appId: UUID, activity: Activity, establishment: String, createdBy: String) {
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
        LocalDateTime.now(ZoneOffset.UTC),
      ),
    )
    println("history = $history")
    println("history list = ${historyRepository.findByAppIdAndEstablishment(appId, establishment)}")
  }

  private fun convertHistoryEntityToHistory(history: List<History>): List<HistoryResponse> {
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
        if (h.activity == Activity.APP_FORWARDED_TO_A_GROUP) {
          convertActivityToReadableMessage(h.activity, "${createdBy.fullName} to group $groupName")
        } else {
          convertActivityToReadableMessage(h.activity, createdBy.fullName)
        },
        h.createdDate,
      )
      list.add(historyResponse)
    }
    return list
  }

  private fun convertActivityToReadableMessage(activity: Activity, staffName: String): String {
    val x: String
    when (activity) {
      Activity.APP_SUBMITTED -> x = "App request submitted by $staffName"
      Activity.APP_REQUEST_FORM_DATA_UPDATED -> x = "App request updated by $staffName"
      Activity.COMMENT_ADDED -> x = "Comment added by $staffName"
      Activity.FORWARDING_COMMENT_ADDED, Activity.APP_FORWARDED_TO_A_GROUP -> x = "Forwarding comment added by $staffName"
      Activity.APP_APPROVED -> x = "App request Approved by $staffName"
      Activity.APP_DECLINED -> x = "App Declined by $staffName"
      else -> throw ApiException("Unknown activity type: $activity", HttpStatus.INTERNAL_SERVER_ERROR)
    }
    return x
  }
}
