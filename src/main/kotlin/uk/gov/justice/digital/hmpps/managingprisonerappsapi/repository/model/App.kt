package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.model

import java.time.LocalDateTime
import java.util.UUID
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

data class App(
  // @Id
  val id: UUID,
  val reference: String,
  val assignedGroup: Group,
  val appType: AppType,
  val createdDate: LocalDateTime,
  val lastModifiedDateTime: LocalDateTime,
  val lastModifiedBy: User,
  val comments: List<Comment>,
  val requests: List<Map<String, JvmType.Object>>,
  val requestedDateTime: LocalDateTime,
  val requestedBy: Prisoner
)
