package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrnApp
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrnAppHistory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.SarContent
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.HistoryRepository
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

class SarService(val appRepository: AppRepository, val historyRepository: HistoryRepository) : HmppsPrisonSubjectAccessRequestService {
  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    var apps = appRepository.findAppsByRequestedBy(prn)
    apps = apps.stream().filter { app ->
      (fromDate == null || fromDate.atStartOfDay() <= app.lastModifiedDate) &&
        (toDate == null || toDate.atStartOfDay() >= app.lastModifiedDate)
    }.toList()
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
            history.activity,
            history.createdDate,
            history.createdBy,
          ),
        )
      }
      val prnApp = PrnApp(
        app.id,
        app.status,
        app.appType,
        app.requestedDate,
        app.establishmentId,
        prnApphistory,
        app.requests,
      )
      list.add(prnApp)
    }
    return SarContent(firstName, lastName, prisonerId, list)
  }
}
