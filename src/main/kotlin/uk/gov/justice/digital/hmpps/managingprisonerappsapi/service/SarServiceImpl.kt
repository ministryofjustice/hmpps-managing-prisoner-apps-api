package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.AttachmentHeader
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.FormDataItem
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrnApp
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrnAppAttachment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrnAppComment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrnAppHistory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrnAppResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.SarContent
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppFileRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ApplicationTypeRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.HistoryRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.ResponseRepository
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
import java.util.UUID

@Service
class SarServiceImpl(
  private val appRepository: AppRepository,
  private val applicationTypeRepository: ApplicationTypeRepository,
  private val historyRepository: HistoryRepository,
  private val appFileRepository: AppFileRepository,
  private val groupRepository: GroupRepository,
  private val commentRepository: CommentRepository,
  private val responseRepository: ResponseRepository,
  @Value("\${hmpps.document.api.url}") private val documentApiurl: String,
  @Value("\${hmpps.service.name}") private val serviceName: String,
) : SarService {

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
    val sarData: SarContent? = convertAppsToSarContent(apps)
    return sarData?.let {
      HmppsSubjectAccessRequestContent(content = sarData)
    }
  }

  override fun convertAppsToSarContent(apps: List<App>): SarContent? {
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
            convertActivityToStatement(history.activity, history.entityId),
            history.createdDate,
            history.createdBy,
          ),
        )
      }

      val comments = commentRepository.getCommentsByAppIdOrderByCreatedDateDesc(app.id)
      val prnAppComments = mutableListOf<PrnAppComment>()
      comments.forEach { comment ->
        prnAppComments.add(
          PrnAppComment(
            comment.message,
            comment.createdBy,
            comment.createdDate,
          ),
        )
      }

      val responses = responseRepository.findByAppId(app.id)
      val prnAppResponses = mutableListOf<PrnAppResponse>()
      if (responses.isNotEmpty()) {
        responses.forEach { response ->
          prnAppResponses.add(
            PrnAppResponse(
              response.decision,
              response.createdBy,
              response.reason,
            ),
          )
        }
      }

      val attachments = appFileRepository.findByAppId(app.id)
      val appAttachments = mutableListOf<PrnAppAttachment>()
      if (attachments.isNotEmpty()) {
        val attachmentHeaderList: List<AttachmentHeader> = listOf(AttachmentHeader("Service-Name", serviceName))

        attachments.forEach { attachment ->
          appAttachments.add(
            PrnAppAttachment(
              attachment.fileType,
              // TODO
              // documentApiurl +  "/documents/" + UUID.fromString(attachment.documentId) + "/file",
              "https://dummyimage.com/350x200/1a1a1a/ffffff&text=New%20Pin%20Phone%20request",
              1200, // TODO -Actual size from DB
              attachmentHeaderList,
            ),
          )
        }
      }

      val assignedGroup = groupRepository.findById(app.assignedGroup)
      val appType = applicationTypeRepository.findById(app.applicationType!!)

      // Convert formData maps to FormDataItem list
      val formDataItems = app.requests.flatMap { requestMap ->
        requestMap.entries.map { entry ->
          FormDataItem(
            key = entry.key,
            value = entry.value.toString(),
          )
        }
      }

      val prnApp = PrnApp(
        app.id,
        app.status,
        appType.get().name,
        app.requestedDate,
        app.lastModifiedDate,
        app.establishmentId,
        assignedGroup.get().name,
        prnApphistory,
        formDataItems,
        prnAppComments,
        prnAppResponses,
        appAttachments,
      )
      list.add(prnApp)
    }
    val sarContent = SarContent(firstName, lastName, prisonerId, list)
    return sarContent
  }

  override fun convertActivityToStatement(activity: Activity, entityId: UUID): String {
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
    } else if (activity == Activity.PRISONER_ID_UPDATE) {
      return "Prisoner Id updated"
    } else if (activity == Activity.FILE_ADDED) {
      val file = appFileRepository.findById(entityId)
      var fileName = ""
      if (file.isPresent) {
        fileName = file.get().fileName
      }
      return "File $fileName added to app request"
    } else {
      throw ApiException("Activity not found", HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }
}
