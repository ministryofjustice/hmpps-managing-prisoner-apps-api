package uk.gov.justice.digital.hmpps.managingprisonerappsapi.stats

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.CommentVisibility
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.LinkedHashMap
import kotlin.toString

/**
 * Sends statistics as custom events to Application Insights.
 *
 * Each method corresponds to a specific requirement from the stats tracking matrix.
 * Every event includes a [STATS_SCHEMA_VERSION] dimension so KQL queries can handle
 * any future schema evolution gracefully.
 *
 * All events are prefixed with "STATS_" (via [StatsActivity]) to separate them from
 * operational telemetry data, making KQL filtering straightforward:
 *
 *   customEvents | where name startswith "STATS_"
 *
 * Common dimensions present on every event:
 *   - schemaVersion   : version of the stats event schema
 *   - appId           : UUID of the app this event relates to
 *   - establishment   : prison establishment code
 *   - appTypeId       : numeric app type identifier
 *   - appTypeName     : human-readable app type name (denormalised for query convenience)
 *   - appGroupId      : numeric app group identifier
 *   - appGroupName    : human-readable app group name (denormalised for query convenience)
 *   - eventTimestamp  : ISO-8601 timestamp of when the event occurred
 */
@Service
class StatsTelemetryService(private val telemetryClient: TelemetryClient) {

  companion object {
    private val log = LoggerFactory.getLogger(StatsTelemetryService::class.java)
    private val FORMATTER = DateTimeFormatter.ISO_DATE_TIME

    /** Bump this when the set of dimensions for any event changes. */
    const val STATS_SCHEMA_VERSION = "1"
  }

  // ---------------------------------------------------------------------------
  // Req 1 + Req 2 — Journey events received from the Frontend
  // ---------------------------------------------------------------------------

  /**
   * Processes the raw list of frontend journey events received after an app is submitted
   * and emits the appropriate STATS_ events to Application Insights.
   *
   * Frontend sends 4 events in order:
   *   1. app_group_viewed        — prisoner opens the taxonomy (group level)
   *   2. app_type_viewed         — prisoner selects the app type
   *   3. app_creation_page_viewed — prisoner opens the creation form
   *   4. app_submitted           — prisoner submits the form
   *
   * This method derives:
   *   - STATS_TAXONOMY_NAVIGATION_TIME  (app_group_viewed → app_type_viewed)         Req 1
   *   - STATS_APP_CREATION_TIME         (app_creation_page_viewed → app_submitted)   Req 2
   *
   * Either stats event is only emitted when both bounding timestamps are present in the list.
   * Unknown event names in the list are silently ignored.
   *
   * KQL — both derived metrics at once:
   *   customEvents
   *   | where name in ("STATS_TAXONOMY_NAVIGATION_TIME", "STATS_APP_CREATION_TIME")
   *   | extend durationSec = toint(customDimensions.durationMs) / 1000.0
   *   | summarize avg(durationSec) by name, tostring(customDimensions.appTypeName)
   */

  fun logJourneyEvents(
    appStatsContext: AppStatsContext,
    request: AppJourneyEventsRequest,
  ) {
    // Build a lookup: FrontendEventType → timestamp (last occurrence wins if duplicated)
    val eventMap = request.events.associate { dto -> dto.event to dto.timestamp }

    // Req 1: taxonomy navigation time
    val groupViewed = eventMap[FrontendEventType.APP_GROUP_VIEWED]
    val typeViewed = eventMap[FrontendEventType.APP_TYPE_VIEWED]
    if (groupViewed != null && typeViewed != null) {
      logTaxonomyNavigationTime(
        appStatsContext,
        taxonomyStartedAt = groupViewed,
        taxonomyEndedAt = typeViewed,
      )
    } else {
      log.debug(
        "Skipping STATS_TAXONOMY_NAVIGATION_TIME for appId=${appStatsContext.appId} " +
          "(app_group_viewed=${groupViewed != null}, app_type_viewed=${typeViewed != null})",
      )
    }

    // Req 2: app creation form time
    val formOpened = eventMap[FrontendEventType.APP_CREATION_PAGE_VIEWED]
    val appSubmitted = eventMap[FrontendEventType.APP_SUBMITTED]
    if (formOpened != null && appSubmitted != null) {
      logAppCreationTime(
        appStatsContext,
        formOpenedAt = formOpened,
        formSubmittedAt = appSubmitted,
      )
    } else {
      log.debug(
        "Skipping STATS_APP_CREATION_TIME for appId=$appStatsContext.appId, " +
          "(app_creation_page_viewed=${formOpened != null}, app_submitted=${appSubmitted != null})",
      )
    }
  }

  // Req 1 – Taxonomy navigation time
  /**
   * Logs the time (milliseconds) a prisoner spent navigating the category taxonomy
   * before reaching the correct app type.
   *
   * App Insights event name: STATS_TAXONOMY_NAVIGATION_TIME
   * Key dimension for querying: durationMs
   *
   *   KQL:
   *   customEvents
   *   | where name == "STATS_TAXONOMY_NAVIGATION_TIME"
   *   | extend duration = toint(customDimensions.durationMs)
   *   | summarize avg(duration), percentile(duration, 90) by tostring(customDimensions.establishment)
   */
  private fun logTaxonomyNavigationTime(
    appStatsContext: AppStatsContext,
    taxonomyStartedAt: LocalDateTime,
    taxonomyEndedAt: LocalDateTime,
  ) {
    val durationMs = Duration.between(taxonomyStartedAt, taxonomyEndedAt).toMillis()
    track(
      StatsActivity.STATS_TAXONOMY_NAVIGATION_TIME,
      appStatsContext,
      taxonomyEndedAt,
    ) {
      it["durationMs"] = durationMs.toString()
      it["taxonomyStartedAt"] = taxonomyStartedAt.format(FORMATTER)
      it["taxonomyEndedAt"] = taxonomyEndedAt.format(FORMATTER)
    }
  }

  // Req 2 – App creation form time
  /**
   * Logs the time (milliseconds) a prisoner spent filling in the app creation form.
   *
   * App Insights event name: STATS_APP_CREATION_TIME
   * Key dimension for querying: durationMs
   *
   * Example KQL:
   *   customEvents
   *   | where name == "STATS_APP_CREATION_TIME"
   *   | extend duration = toint(customDimensions.durationMs)
   *   | summarize avg(duration) by tostring(customDimensions.appTypeName), bin(timestamp, 1d)
   */
  private fun logAppCreationTime(
    appStatsContext: AppStatsContext,
    formOpenedAt: LocalDateTime,
    formSubmittedAt: LocalDateTime,
  ) {
    val durationMs = Duration.between(formOpenedAt, formSubmittedAt).toMillis()
    track(
      StatsActivity.STATS_APP_CREATION_TIME,
      appStatsContext,
      formSubmittedAt,
    ) {
      it["durationMs"] = durationMs.toString()
      it["formOpenedAt"] = formOpenedAt.format(FORMATTER)
      it["formSubmittedAt"] = formSubmittedAt.format(FORMATTER)
    }
  }

  // ---------------------------------------------------------------------------
  // Req 3 – Staff decision time
  // ---------------------------------------------------------------------------
  /**
   * Logs the time (milliseconds) from app submission until a staff member makes
   * a decision (approve / decline / reject).
   *
   * Called by the BE when a response is recorded. [appCreatedAt] is retrieved from the
   * persisted app; [decisionMadeAt] is the current timestamp.
   *
   * App Insights event name: STATS_APP_DECISION_TIME
   * Key dimensions: durationMs, decisionStatus
   * decisionStatus could be 1 of APPROVED, DECLINED, REJECTED
   *
   * Example KQL:
   *   customEvents
   *   | where name == "STATS_APP_DECISION_TIME"
   *   | extend duration = toint(customDimensions.durationMs)
   *   | summarize avg(duration) by tostring(customDimensions.decisionStatus), tostring(customDimensions.establishment)
   */
  fun logAppDecisionTime(
    appStatsContext: AppStatsContext,
    decisionStatus: AppStatus,
    appCreatedAt: LocalDateTime,
    decisionMadeAt: LocalDateTime,
  ) {
    val durationMs = Duration.between(appCreatedAt, decisionMadeAt).toMillis()
    track(
      StatsActivity.STATS_APP_DECISION_TIME,
      appStatsContext,
      decisionMadeAt,
    ) {
      it["decisionStatus"] = decisionStatus.toString()
      it["durationMs"] = durationMs.toString()
      it["submittedAt"] = appCreatedAt.format(FORMATTER)
      it["decisionMadeAt"] = decisionMadeAt.format(FORMATTER)
    }
  }

  // ---------------------------------------------------------------------------
  // Req 4 – App submitted (app-type usage)
  // ---------------------------------------------------------------------------
  /**
   * Logs the submission of an app. Used to track which app types are most used,
   * per establishment, over time.
   *
   * Called by the BE immediately after the app is persisted.
   * App Insights event name: STATS_APP_SUBMITTED
   *
   * KQL:
   *   customEvents
   *   | where name == "STATS_APP_SUBMITTED"
   *   | summarize count() by tostring(customDimensions.appTypeName), tostring(customDimensions.establishment), bin(timestamp, 7d)
   *   | order by count_ desc
   */
  fun logAppSubmitted(
    appStatsContext: AppStatsContext,
    submittedAt: LocalDateTime,
  ) {
    track(
      StatsActivity.STATS_APP_SUBMITTED,
      appStatsContext,
      submittedAt,
    ) {
    }
  }

  // ---------------------------------------------------------------------------
  // Req 5 – Status change journey
  // ---------------------------------------------------------------------------
  /**
   * Logs every status transition for an app.
   * Together these events allow the full status journey to be reconstructed per app.
   *
   * Called by the BE on every status change.
   *
   * App Insights event name: STATS_STATUS_CHANGED
   * Key dimensions: fromStatus, toStatus
   * AppStatus Values - NEW,IN_PROGRESS,APPROVED,DECLINED,REJECTED,
   *
   * KQL — most common status journeys:
   *   customEvents
   *   | where name == "STATS_STATUS_CHANGED"
   *   | extend journey = strcat(tostring(customDimensions.fromStatus), " → ", tostring(customDimensions.toStatus))
   *   | summarize count() by journey, tostring(customDimensions.appTypeName)
   *   | order by count_ desc
   */
  fun logStatusChanged(
    appStatsContext: AppStatsContext,
    fromStatus: AppStatus,
    toStatus: AppStatus,
    changedAt: LocalDateTime,
  ) {
    track(
      StatsActivity.STATS_STATUS_CHANGED,
      appStatsContext,
      changedAt,
    ) {
      it["fromStatus"] = fromStatus.name
      it["toStatus"] = toStatus.name
    }
  }

  // ---------------------------------------------------------------------------
  // Req 6 – Messages per app type
  // ---------------------------------------------------------------------------

  /**
   * Logs that a message (comment) was added to an app.
   *
   * App Insights event name: STATS_MESSAGE_ADDED
   * Key dimension: messageType (e.g. PRISONER_COMMENT, STAFF_NOTE, FORWARDING_COMMENT)
   *
   * Example KQL:
   *   customEvents
   *   | where name == "STATS_MESSAGE_ADDED"
   *   | summarize count() by tostring(customDimensions.messageType), tostring(customDimensions.appTypeName)
   */
  fun logMessageAdded(
    appStatsContext: AppStatsContext,
    addedBy: UserCategory,
    visibleTo: CommentVisibility,
    messageType: MessageType,
    addedAt: LocalDateTime,
  ) {
    track(
      StatsActivity.STATS_MESSAGE_ADDED,
      appStatsContext,
      addedAt,
    ) {
      it["addedBy"] = addedBy.name
      it["visibleTo"] = visibleTo.name
      it["messageType"] = messageType.name
    }
  }

  // ---------------------------------------------------------------------------
  // Req 7 – App rejected (with rejection reason)
  // ---------------------------------------------------------------------------

  /**
   * Logs an app rejection, capturing the rejection reason in full.
   * This is a dedicated event (in addition to STATS_STATUS_CHANGED) so rejection reasons
   * can be queried directly without filtering another event type.
   *
   * App Insights event name: STATS_APP_REJECTED
   * Key dimension: rejectionReason
   *
   * Example KQL:
   *   customEvents
   *   | where name == "STATS_APP_REJECTED"
   *   | summarize count() by tostring(customDimensions.rejectionReason), tostring(customDimensions.appTypeName)
   *   | order by count_ desc
   */
  fun logAppRejected(
    appStatsContext: AppStatsContext,
    rejectionReason: String,
    rejectedAt: LocalDateTime,
  ) {
    track(
      StatsActivity.STATS_APP_REJECTED,
      appStatsContext,
      rejectedAt,
    ) {
      it["rejectionReason"] = rejectionReason
    }
  }

  /**
   * Builds the common dimensions present on every stats event and calls [telemetryClient.trackEvent].
   * The [extraDimensions] lambda receives the map and adds event-specific fields.
   */
  private fun track(
    activity: StatsActivity,
    appStatsContext: AppStatsContext,
    eventTimestamp: LocalDateTime,
    extraDimensions: (LinkedHashMap<String, String>) -> Unit,
  ) {
    try {
      val dimensions = LinkedHashMap<String, String>()
      // Common fields on every stats event
      dimensions["schemaVersion"] = STATS_SCHEMA_VERSION
      dimensions["appId"] = appStatsContext.appId.toString()
      dimensions["establishment"] = appStatsContext.establishment
      dimensions["appTypeId"] = appStatsContext.appTypeId.toString()
      dimensions["appTypeName"] = appStatsContext.appTypeName
      dimensions["appGroupId"] = appStatsContext.appGroupId.toString()
      dimensions["appGroupName"] = appStatsContext.appGroupName
      dimensions["department"] = appStatsContext.department
      dimensions["eventTimestamp"] = eventTimestamp.format(FORMATTER)
      // Event-specific fields
      extraDimensions(dimensions)

      telemetryClient.trackEvent(activity.name, dimensions, null)
      log.debug("telemetry for [${activity.name}] - {}", dimensions)
    } catch (e: Exception) {
      log.error("Failed to send stats telemetry event [${activity.name}] for appId=${appStatsContext.appId}: ${e.message}", e)
    }
  }
}
