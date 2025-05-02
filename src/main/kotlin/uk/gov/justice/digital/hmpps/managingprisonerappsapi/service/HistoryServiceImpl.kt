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
) : HistoryService {
  companion object {
    val logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun getHistoryByAppId(appId: UUID, user: String): List<HistoryResponse> {
    val staff = staffService.getStaffById(user).orElseThrow {
      ApiException("Staff with id $user does not exist", HttpStatus.FORBIDDEN)
    }
    val history = historyRepository.findHistoryByAppIdAndEstablishment(appId, staff.establishmentId)
    return convertHistoryEntityToHistory(history)
  }

  override fun updateActivityInHistory(entityId: UUID, entityType: EntityType, appId: UUID, activity: Activity, establishment: String, createdBy: String) {
    logger.info("Updating history for entity:$entityType, activity: $activity,  entityType: $entityType, appId: $appId")
    historyRepository.save(
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
  }

  private fun convertHistoryEntityToHistory(history: List<History>): List<HistoryResponse> {
    val list = mutableListOf<HistoryResponse>()
    history.forEach { h ->
      val createdBy = staffService.getStaffById(h.createdBy).orElseThrow {
        ApiException("Staff with id ${h.createdBy} does not exist", HttpStatus.NOT_FOUND)
      }
      val historyResponse = HistoryResponse(
        h.id,
        h.appId,
        createdBy.fullName,
        h.createdDate,
        h.establishment,
        h.activity.toString(),
      )
      list.add(historyResponse)
    }
    return list
  }
}
