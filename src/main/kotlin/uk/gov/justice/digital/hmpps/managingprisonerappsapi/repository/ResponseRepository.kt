package uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Response
import java.util.UUID

@Repository
interface ResponseRepository : JpaRepository<Response, UUID> {

  @Query(
    value = "SELECT r.* FROM app_responses ar " +
      " inner join response r on r.id =ar.responses " +
      "WHERE ar.app_id = :appId",
    nativeQuery = true,
  )
  fun findByAppId(appId: UUID): List<Response>
}
