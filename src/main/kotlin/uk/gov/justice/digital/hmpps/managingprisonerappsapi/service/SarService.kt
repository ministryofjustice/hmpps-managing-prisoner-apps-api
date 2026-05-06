package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.response.SarContentAndAttachments
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Activity
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import java.util.UUID

/**
 * Service for handling Subject Access Request (SAR) operations.
 * Provides functionality to retrieve and convert prisoner app data for SAR purposes.
 */
interface SarService : HmppsPrisonSubjectAccessRequestService {

  /**
   * Converts a list of apps to SAR content with attachments.
   * 
   * @param apps List of apps to convert
   * @return SarContentAndAttachments containing the SAR content and associated attachments, or null if the list is empty
   */
  fun convertAppsToSarContent(apps: List<App>): SarContentAndAttachments?

  /**
   * Converts an activity to a human-readable statement.
   * 
   * @param activity The activity to convert
   * @param entityId The entity ID associated with the activity
   * @return Human-readable description of the activity
   * @throws uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException if activity is not found
   */
  fun convertActivityToStatement(activity: Activity, entityId: UUID): String
}
