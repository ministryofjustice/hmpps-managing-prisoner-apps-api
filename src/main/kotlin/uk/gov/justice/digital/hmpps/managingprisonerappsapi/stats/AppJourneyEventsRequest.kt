package uk.gov.justice.digital.hmpps.managingprisonerappsapi.stats

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * Request body for `POST /v1/apps/{appId}/journey-events`.
 *
 * The Frontend sends this after an app has been successfully submitted.
 * The Backend derives two stats events from this list:
 *  - `STATS_TAXONOMY_NAVIGATION_TIME`  (app_group_viewed → app_type_viewed)       — Req 1
 *  - `STATS_APP_CREATION_TIME`         (app_creation_page_viewed → app_submitted) — Req 2
 *
 * Example payload:
 * {
 *   "appId": "UUID",
 *   "events": [
 *     { "event": "app_group_viewed",        "timestamp": "2026-08-11T08:40:55.998Z" },
 *     { "event": "app_type_viewed",         "timestamp": "2026-08-11T08:40:58.181Z" },
 *     { "event": "app_creation_page_viewed","timestamp": "2026-08-11T08:42:01.113Z" },
 *     { "event": "app_submitted",           "timestamp": "2026-08-11T08:45:10.283Z" }
 *   ]
 * }
 */
data class AppJourneyEventsRequest(
  @Schema(description = "App Id", example = "UUID")
  val appId: UUID,

  @Schema(description = "Ordered list of frontend journey events captured during app creation")
  val events: List<FrontendJourneyEventDto>,
)
