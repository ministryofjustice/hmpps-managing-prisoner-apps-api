package uk.gov.justice.digital.hmpps.managingprisonerappsapi.resource

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.nimbusds.jose.Algorithm
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.repository.AppRepository
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils.DataGenerator
import java.net.URI
import java.security.KeyPair
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.*


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AppResourceIntegrationTest(@Autowired private var appRepository: AppRepository) {
  @LocalServerPort
  private val port = 0

  private val baseUrl = "http://localhost"

  private val restTemplate: RestTemplate = RestTemplate()

  private val wiremock = WireMockServer(8085)

  var keyPair: KeyPair? = null
  var privateKey: String = ""
  var token: String = ""

  @BeforeEach
  fun setUp() {
    val keyPair = DataGenerator.generateRandomRSAKey()
    privateKey = Base64.getEncoder().encodeToString(keyPair.private.encoded)
    val publicKey = Base64.getEncoder().encodeToString(keyPair.public.encoded)
    println(privateKey)
    println(publicKey)
    val kid = UUID.randomUUID().toString()
    val jwk: JWK = RSAKey.Builder(keyPair.public as RSAPublicKey)
      //.privateKey(keyPair.private as RSAPrivateKey)
      .algorithm(Algorithm.parse("RS256"))
      .keyUse(KeyUse.SIGNATURE)
      .keyID(kid)
      .issueTime(Date())
      .build()



    println(String.format("{\"keys\":[%s]}", jwk.toPublicJWK().toJSONString()))
    token = DataGenerator.jwtBuilder(
      Instant.now(),
      Instant.now().plusSeconds(3600),
      kid,
      "hjgt",
      privateKey,
    )
    println(token)
    val jwks = jwk.toPublicJWK().toString()
    wiremock.start()
    WireMock.configureFor("localhost", wiremock.port())
    WireMock.stubFor(
      WireMock.get(WireMock.urlEqualTo("/auth/.well-known/jwks.json")).willReturn(
        WireMock.aResponse()
          .withStatus(HttpStatus.OK.value())
          .withHeader("Content-Type", "application/json")
          .withBody(
            String.format("{\"keys\":[%s]}", jwks)
          ),
      ),
    )
  }

  @AfterEach
  fun tearOff() {
    appRepository.deleteAll()
    wiremock.stop()
  }

  //@Test
  fun `submit an app`() {


    val instant = Instant.now()
    println(token)
    var headers = LinkedMultiValueMap<String, String>()
    headers.add("Authorization", "Bearer $token")
    headers.add("Content-Type", "application/json")
    headers.add("Accept", "application/json")
    var url = URI("$baseUrl:$port/v1/prisoners/G12345/apps")
    val app = DataGenerator.generateApp()
    var response = restTemplate.exchange(
      RequestEntity<Any>(app, headers, HttpMethod.POST, url),
      object : ParameterizedTypeReference<Any>() {},
    )
    Assertions.assertEquals(200, response.statusCode.value())
  }

  //@Test
  fun `get an app by id`() {
    var headers = LinkedMultiValueMap<String, String>()
    headers.add("Authorization", "Bearer $token")
    headers.add("Content-Type", "application/json")
    headers.add("Accept", "application/json")
    var url = URI("$baseUrl:$port/v1/prisoners/G12345/apps/12345")
    val app = DataGenerator.generateApp()
    var response = restTemplate.exchange(
      RequestEntity<Any>(app, headers, HttpMethod.GET, url),
      object : ParameterizedTypeReference<Any>() {},
    )
    Assertions.assertEquals(401, response.statusCode.value())
  }
}