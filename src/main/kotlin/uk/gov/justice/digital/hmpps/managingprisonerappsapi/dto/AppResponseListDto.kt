package uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto

import com.fasterxml.jackson.annotation.JsonFormat
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import java.time.LocalDateTime
import java.util.*

data class AppResponseListDto(
    val page: Int,
    val totalRecords: Long,
    val exhausted: Boolean,
    val appTypes: Map<AppType, Int>,
    val apps: List<AppListViewDto>,
)

data class GroupAppListViewDto(
    val id: UUID,
    val name: String,
)

data class AppListViewDto(
    val id: UUID,
    val establishmentId: String,
    val status: String,
    val appType: String,
    val requestedBy: String,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val requestedDate: LocalDateTime,
    val assignedGroup: Any,
)

