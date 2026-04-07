package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.FormDataItem
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrnApp
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrnAppAttachment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrnAppComment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrnAppHistory
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.PrnAppResponse
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.SarContent
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.SarContentAndAttachments
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
import uk.gov.justice.hmpps.kotlin.sar.Attachment
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
import java.util.UUID

@Service
class SarService(
  val appRepository: AppRepository,
  val applicationTypeRepository: ApplicationTypeRepository,
  val historyRepository: HistoryRepository,
  val appFileRepository: AppFileRepository,
  val groupRepository: GroupRepository,
  val commentRepository: CommentRepository,
  val responseRepository: ResponseRepository,
  @Value("\${hmpps.document.api.url}") val documentApiurl: String,
  @Value("\${hmpps.service.name}") val serviceName: String,
) : HmppsPrisonSubjectAccessRequestService {

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
    val sarData: SarContentAndAttachments? = convertAppsToSarContent(apps)
    return HmppsSubjectAccessRequestContent(content = sarData?.content as Any, attachments = sarData?.attachments)
  }

  fun convertAppsToSarContent(apps: List<App>): SarContentAndAttachments? {
    if (apps.isEmpty()) return null
    val list = mutableListOf<PrnApp>()
    val firstName = apps.get(0).requestedByFirstName
    val lastName = apps.get(0).requestedByLastName
    val prisonerId = apps.get(0).requestedBy
    val allPrnAttachments = mutableListOf<Attachment>()

    apps.forEach { app ->
      var fileCount: Int = 1
      val histories = historyRepository.findByAppIdAndEstablishmentOrderByCreatedDate(app.id, app.establishmentId)
      val prnApphistory = mutableListOf<PrnAppHistory>()
      histories.forEach { history ->
        prnApphistory.add(
          PrnAppHistory(
            convertActivityToStatement(history.activity, history.entityId),
            history.createdDate,
            // history.createdBy,
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
      var prnAppResponses = mutableListOf<PrnAppResponse>()
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
        attachments.forEach { attachment ->
          appAttachments.add(
            PrnAppAttachment(
              app.id,
              serviceName,
              fileCount,
              attachment.fileName,
              "Scanned Sheet",
              documentApiurl + attachment.fileName + "/" + UUID.fromString(attachment.documentId),
              1200, // TODO -Actual size from DB
              attachment.fileType,
            ),
          )
          allPrnAttachments.add(
            Attachment(
              // Ref No. //TODO
              // Namespace //TODO
              fileCount,
              "Scanned Sheet", // TODO File Name from DB
              attachment.fileType,
              documentApiurl + attachment.fileName + "/" + UUID.fromString(attachment.documentId),
              1200, // TODO - Actual size from DB
              attachment.fileName,
            ),
          )
          fileCount++
        }
      }

      val assignedGroup = groupRepository.findById(app.assignedGroup)
      val appType = applicationTypeRepository.findById(app.applicationType!!)

      // Convert formData maps to FormDataItem list
      val formDataItems = app.requests.flatMap { requestMap ->
        requestMap.entries.map { entry ->
          FormDataItem(
            key = entry.key,
            value = entry.value?.toString() ?: "",
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
        app.requests,
        formDataItems,
        prnAppComments,
        prnAppResponses,
        appAttachments,
      )
      list.add(prnApp)
    }
    val sarContent = SarContent(firstName, lastName, prisonerId, list)
    return SarContentAndAttachments(sarContent, allPrnAttachments)
  }

  private fun convertActivityToStatement(activity: Activity, entityId: UUID): String {
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
