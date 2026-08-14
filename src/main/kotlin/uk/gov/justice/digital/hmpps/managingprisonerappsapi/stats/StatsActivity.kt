package uk.gov.justice.digital.hmpps.managingprisonerappsapi.stats

/**
 * Event names used by StatsTelemetryService when sending statistics custom events to Application Insights.
 * Each value maps 1-to-1 to a business requirement and produces a queryable custom event.
 *
 * Requirement coverage:
 *  Req 1  → STATS_TAXONOMY_NAVIGATION_TIME
 *  Req 2  → STATS_APP_CREATION_TIME
 *  Req 3  → STATS_APP_DECISION_TIME
 *  Req 4  → STATS_APP_SUBMITTED
 *  Req 5  → STATS_STATUS_CHANGED
 *  Req 6  → STATS_MESSAGE_ADDED
 *  Req 7  → STATS_APP_REJECTED  (rejection reason captured here)
 */
enum class StatsActivity {
  /** Req 1 – Time (ms) a prisoner spends navigating the taxonomy before reaching the target app type. */
  STATS_TAXONOMY_NAVIGATION_TIME,

  /** Req 2 – Time (ms) a prisoner spends on the app creation form before submitting. */
  STATS_APP_CREATION_TIME,

  /** Req 3 – Time (ms) from app submission until a staff member makes a decision. */
  STATS_APP_DECISION_TIME,

  /** Req 4 – An app was formally submitted. Used to track app-type usage per establishment over time. */
  STATS_APP_SUBMITTED,

  /** Req 5 – An app's status changed. Captures the full status journey (from → to). */
  STATS_STATUS_CHANGED,

  /** Req 6 – A message (comment) was added to an app. */
  STATS_MESSAGE_ADDED,

  /** Req 7 – An app was rejected; the rejection reason is captured directly on this event. */
  STATS_APP_REJECTED,
}
