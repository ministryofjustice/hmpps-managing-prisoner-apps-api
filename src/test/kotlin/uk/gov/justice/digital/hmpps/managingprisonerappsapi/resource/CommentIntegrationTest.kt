package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.CommentRequestDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.CommentResponseDto
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.dto.PageResultComments
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.integration.wiremock.PrisonerSearchApiExtension.Companion.prisonerSearchApi
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Establishment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.GroupType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.CommentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.EstablishmentRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class CommentIntegrationTest(
  @Autowired private val appRepository: AppRepository,
  @Autowired private val groupRepository: GroupRepository,
  @Autowired private val establishmentRepository: EstablishmentRepository,
  @Autowired private val commentRepository: CommentRepository,
) : IntegrationTestBase() {

  private lateinit var appId: UUID

  companion object {
    val establishmentIdFirst = "TEST_ESTABLISHMENT_FIRST"
    val establishmentIdSecond = "TEST_ESTABLISHMENT_SECOND"
    val establishmentIdThird = "TEST_ESTABLISHMENT_THIRD"
    val assignedGroupFirst = UUID.randomUUID()
    val assignedGroupFirstName = "Business Hub"
    val assignedGroupSecond = UUID.randomUUID()
    val assignedGroupSecondName = "OMU"
    val requestedByFirst = "A12345"
    val requestedByFirstMainName = "John"
    val requestedByFirstSurname = "Smith"
    val requestedBySecondMainName = "John"
    val requestedBySecondSurname = "Butler"
    val requestedBySecond = "B12345"
    val requestedByThird = "C12345"
    val requestedByThirdMainName = "Test"
    val requestedByThirdSurname = "User"

  }

  @BeforeEach
  fun setup() {
    appRepository.deleteAll()
    groupRepository.deleteAll()
    establishmentRepository.deleteAll()
    commentRepository.deleteAll()
    populateEstablishments()
    populateGroups()
    populateApps()

    prisonerSearchApi.start()
    prisonerSearchApi.stubPrisonerSearchFound()

    webTestClient = webTestClient
      .mutate()
      .responseTimeout(Duration.ofMillis(60000))
      .build()
  }

  @AfterEach
  fun teardown() {
    appRepository.deleteAll()
    groupRepository.deleteAll()
    establishmentRepository.deleteAll()
    commentRepository.deleteAll()
  }

  @Test
  fun `add a comment for the app`() {
    val message = "This needs to be checked again"
    val body = CommentRequestDto(
      message
    )
    val response = webTestClient.post()
      .uri("/v1/prisoners/G9737VL/apps/$appId/comments")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(body)
      .exchange()
      .expectStatus().isCreated
      .expectBody(object : ParameterizedTypeReference<CommentResponseDto<String>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as CommentResponseDto<String>

    Assertions.assertNotNull(message, response.message)
    Assertions.assertEquals(appId, response.appId)
    Assertions.assertEquals("G9737VL", response.prisonerNumber)
  }

  @Test
  fun `get comment by id`() {
    val message = "This needs to be checked again"
    val body = CommentRequestDto(
      message
    )
    val response = webTestClient.post()
      .uri("/v1/prisoners/G9737VL/apps/$appId/comments")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(body)
      .exchange()
      .expectStatus().isCreated
      .expectBody(object : ParameterizedTypeReference<CommentResponseDto<String>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as CommentResponseDto<String>

    Assertions.assertNotNull(message, response.message)
    Assertions.assertEquals(appId, response.appId)
    Assertions.assertEquals("G9737VL", response.prisonerNumber)

    var res = webTestClient.get()
      .uri("/v1/prisoners/G9737VL/apps/$appId/comments/${response.id}")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectBody(object : ParameterizedTypeReference<CommentResponseDto<String>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as CommentResponseDto<String>

    Assertions.assertNotNull(message, res.message)
    Assertions.assertEquals(appId, res.appId)
    Assertions.assertEquals("G9737VL", res.prisonerNumber)

   val resp = webTestClient.get()
      .uri("/v1/prisoners/G9737VL/apps/$appId/comments/${response.id}?createdBy=true")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      //.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectBody(object : ParameterizedTypeReference<CommentResponseDto<Staff>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as CommentResponseDto<Staff>

    Assertions.assertNotNull(message, resp.message)
    Assertions.assertEquals(appId, resp.appId)
    Assertions.assertEquals("G9737VL", resp.prisonerNumber)
  }

  @Test
  fun `get comments by app id`() {
    val message = "This needs to be checked again"
    val body = CommentRequestDto(
      message
    )
    val response = webTestClient.post()
      .uri("/v1/prisoners/G9737VL/apps/$appId/comments")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(body)
      .exchange()
      .expectStatus().isCreated
      .expectBody(object : ParameterizedTypeReference<CommentResponseDto<String>>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as CommentResponseDto<String>

    var res = webTestClient.get()
      .uri("/v1/prisoners/G9737VL/apps/$appId/comments?page=1&size=10")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectBody(object : ParameterizedTypeReference<PageResultComments>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as PageResultComments

    Assertions.assertEquals(1, res.page)
    Assertions.assertEquals(1, res.totalElements)

    res = webTestClient.get()
      .uri("/v1/prisoners/G9737VL/apps/$appId/comments?page=1&size=10")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGING_PRISONER_APPS")))
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectBody(object : ParameterizedTypeReference<PageResultComments>() {})
      .consumeWith(System.out::println)
      .returnResult()
      .responseBody as PageResultComments

    Assertions.assertEquals(1, res.page)
    Assertions.assertEquals(1, res.totalElements)
  }

  private fun populateEstablishments() {
    establishmentRepository.save(Establishment(establishmentIdFirst, "ESTABLISHMENT_NAME_1"))
  }

  private fun populateGroups() {
    groupRepository.save(
      DataGenerator.generateGroups(
        assignedGroupFirst,
        establishmentIdFirst,
        assignedGroupFirstName,
        listOf(AppType.PIN_PHONE_ADD_NEW_CONTACT, AppType.PIN_PHONE_REMOVE_CONTACT),
        GroupType.WING,
      ),
    )
    groupRepository.save(
      DataGenerator.generateGroups(
        assignedGroupSecond,
        establishmentIdFirst,
        assignedGroupSecondName,
        listOf(AppType.PIN_PHONE_ADD_NEW_CONTACT, AppType.PIN_PHONE_REMOVE_CONTACT),
        GroupType.WING,
      ),
    )
  }

  private fun populateApps() {
    appId = appRepository.save(
      DataGenerator.generateApp(
        establishmentIdFirst,
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        requestedByFirst,
        LocalDateTime.now(ZoneOffset.UTC).minusDays(4),
        requestedByFirstMainName,
        requestedByFirstSurname,
        AppStatus.PENDING,
        assignedGroupFirst,
      )
    ).id
  }
}