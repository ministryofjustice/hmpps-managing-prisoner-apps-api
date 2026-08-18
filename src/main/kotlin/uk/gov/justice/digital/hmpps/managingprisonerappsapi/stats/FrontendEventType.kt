package uk.gov.justice.digital.hmpps.managingprisonerappsapi.stats

import com.fasterxml.jackson.annotation.JsonValue

/**
 * The event names sent by the Frontend as part of the app journey events payload.
 *
 * The BE uses pairs of these to derive stats events:
 *   APP_GROUP_VIEWED → APP_TYPE_VIEWED        = STATS_TAXONOMY_NAVIGATION_TIME
 *   APP_CREATION_PAGE_VIEWED → APP_SUBMITTED  = STATS_APP_CREATION_TIME
 */
enum class FrontendEventType(@JsonValue val value: String) {
  APP_GROUP_VIEWED("app_group_viewed"),
  APP_TYPE_VIEWED("app_type_viewed"),
  APP_CREATION_PAGE_VIEWED("app_creation_page_viewed"),
  APP_SUBMITTED("app_submitted"),
}
