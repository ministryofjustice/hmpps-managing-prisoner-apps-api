package uk.gov.justice.digital.hmpps.managingprisonerappsapi.service

import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Groups
import java.util.UUID

interface GroupService {

  fun getGroupsByEstablishment(establishmentId: UUID, name: String)

  fun getGroupById(id: UUID): Groups
}