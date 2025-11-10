package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrnApp
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrnAppHistory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.SarContent
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.HistoryRepository
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

@Service
class SarService(val appRepository: AppRepository, val historyRepository: HistoryRepository) : HmppsPrisonSubjectAccessRequestService {
  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    var apps = appRepository.findAppsByRequestedBy(prn)
    apps = apps.stream().filter { app ->
      (fromDate == null || fromDate.atStartOfDay() <= app.requestedDate) &&
        (toDate == null || toDate.atStartOfDay() >= app.lastModifiedDate)
    }.toList()
    if (apps.isEmpty()) {
      return null
    }
    val content = convertAppsToSarContent(apps)
    return HmppsSubjectAccessRequestContent(content as Any)
  }

  fun convertAppsToSarContent(apps: List<App>): SarContent? {
    if (apps.isEmpty()) return null
    val list = mutableListOf<PrnApp>()
    val firstName = apps.get(0).requestedByFirstName
    val lastName = apps.get(0).requestedByLastName
    val prisonerId = apps.get(0).requestedBy
    apps.forEach { app ->
      val histories = historyRepository.findByAppIdAndEstablishmentOrderByCreatedDate(app.id, app.establishmentId)
      val prnApphistory = mutableListOf<PrnAppHistory>()
      histories.forEach { history ->
        prnApphistory.add(
          PrnAppHistory(
            convertActivityToStatement(history.activity),
            history.createdDate,
            history.createdBy,
          ),
        )
      }
      val prnApp = PrnApp(
        app.id,
        app.status,
        app.applicationType!!,
        app.requestedDate,
        app.lastModifiedDate,
        app.establishmentId,
        prnApphistory,
        app.requests,
      )
      list.add(prnApp)
    }
    return SarContent(firstName, lastName, prisonerId, list)
  }

  private fun convertActivityToStatement(activity: Activity): String {
    if (activity == Activity.APP_SUBMITTED) {
      return "App request submitted."
    } else if (activity == Activity.APP_DECLINED) {
      return "App request declined."
    } else if (activity == Activity.APP_APPROVED) {
      return "App request approved."
    } else if (activity == Activity.FORWARDING_COMMENT_ADDED) {
      return "Forwarding comment added to app request"
    } else if (activity == Activity.COMMENT_ADDED) {
      return "Comment added to app request ."
    } else if (activity == Activity.APP_FORWARDED_TO_A_GROUP) {
      return "App request forwarded to a approval department."
    } else if (activity == Activity.APP_REQUEST_FORM_DATA_UPDATED) {
      return "App request form data updated"
    } else {
      throw ApiException("Activity not found", HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }
}
