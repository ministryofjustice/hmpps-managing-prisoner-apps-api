package uk.gov.justice.digital.hmpps.managingprisonerappsapi.stats

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * A single journey event sent by the Frontend after an app is submitted.
 * Example payload:
 * ```json
 * { "event": "app_group_viewed", "timestamp": "2026-08-11T08:40:55.998Z" }
 * ```
 */
data class FrontendJourneyEventDto(
  @Schema(
    description = "Snake-case event name from the frontend. " +
      "Supported values: app_group_viewed, app_type_viewed, app_creation_page_viewed, app_submitted",
  )
  val event: FrontendEventType,

  @Schema(
    description = "UTC timestamp of when this event occurred in the browser",
    example = "2026-08-11T08:40:55.998Z",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
  val timestamp: LocalDateTime,
)
