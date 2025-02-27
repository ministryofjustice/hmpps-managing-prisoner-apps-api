package uk.gov.justice.digital.hmpps.managingprisonerappsapi.utils

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.exceptions.ApiException
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.App
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.AppType
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Comment
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Decision
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Response
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.Staff
import uk.gov.justice.digital.hmpps.managingprisonerappsapi.model.UserCategory
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.HashMap

class DataGenerator {
  companion object {
    fun generateComment(createdBy: UUID): Comment {
      return Comment(
        UUID.randomUUID(),
        "Looks good to me to approve",
        LocalDateTime.now(),
        createdBy,
        setOf(UUID.randomUUID()),
        UUID.randomUUID(),
      )
    }

    fun generateResponse(staffId: UUID): Response {
      return Response(
        UUID.randomUUID(),
        "Pass all requirement",
        Decision.APPROVED,
        LocalDateTime.now(),
        staffId,
      )
    }

    fun generateStaff(): Staff {
      return Staff(
        UUID.randomUUID(),
        "Test",
        "Staff",
        UserCategory.STAFF,
        setOf(UUID.randomUUID()),
        "Prison Warden",
      )
    }

    fun generateApp(): App {
      return App(
        UUID.randomUUID(),
        UUID.randomUUID().toString(),
        UUID.randomUUID(),
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        LocalDateTime.now(),
        LocalDateTime.now(),
        UUID.randomUUID(),
        arrayListOf(UUID.randomUUID()),
        listOf(HashMap<String, Any>()),
        LocalDateTime.now(),
        UUID.randomUUID(),
        UUID.randomUUID()
      )
    }

    fun generateRandomRSAKey(): KeyPair {
      val rsaGenerator = KeyPairGenerator.getInstance("RSA")
      rsaGenerator.initialize(2048)


      return rsaGenerator.genKeyPair()
    }

    fun getPrivateKey(secret: String): PrivateKey {
      try {
        val privateKeyFormatted = secret
          .trimIndent()
          .replace("-----BEGIN PRIVATE KEY-----", "")
          .replace("-----END PRIVATE KEY-----", "")
          .replace("\\s".toRegex(), "")
        val privateKeyInBytes = Base64.getDecoder().decode(privateKeyFormatted)
        return KeyFactory.getInstance("RSA").generatePrivate(
          PKCS8EncodedKeySpec(privateKeyInBytes),
        )
      } catch (e: Exception) {
        val message = "Error converting private key string to private key object ${e.message}"
        throw ApiException(message, HttpStatus.FORBIDDEN)
      }
    }

    fun jwtBuilder(issue: Instant, exp: Instant, kid: String, userId: String?, secret: String): String {
      val privateKey = getPrivateKey(secret)
      val issueDate = Date.from(issue)
      val expDate = Date.from(exp)
      return Jwts.builder()
        .setIssuer("http://localhost:8085/auth/issuer")
        .setSubject("login")
        .setAudience("test audience")
        .claim("username", "testuser@test.com")
        .claim("name", "Test User")
        .claim("scope", "openid")
        .claim("kid", kid)
        .claim("preferred_username", userId)
        .claim( "authorities", "[\"ROLE_MANAGING_PRISONER_APPS\",]")
        .claim("tid", "123456_random_value")
        .setIssuedAt(issueDate)
        .setExpiration(expDate)
        .signWith(
          privateKey,
          SignatureAlgorithm.RS256,
        )
        .compact()
    }
  }
}
