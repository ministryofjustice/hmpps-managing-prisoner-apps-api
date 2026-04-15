package uk.gov.justice.digital.hmpps.managingprisonerappsapi.helpers

import org.slf4j.LoggerFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.io.IOException
import java.net.ServerSocket

/**
 * This is a LocalStack container configuration to create a Testcontainers LocalStack instance
 * only if a standalone LocalStack instance is not already running. This means that if you check out the library and
 * run the tests then Testcontainers will jump in and start a LocalStack instance for you.
 */
object LocalStackContainer {
  private val log = LoggerFactory.getLogger(this::class.java)
  val instance by lazy { startLocalstackIfNotRunning() }

  fun setLocalStackProperties(localStackContainer: LocalStackContainer, registry: DynamicPropertyRegistry) {
    val localstackUrl = localStackContainer.endpoint
    val region = localStackContainer.region
    registry.add("hmpps.sqs.localstackUrl") { localstackUrl }
    registry.add("hmpps.sqs.region") { region }
  }

  private fun startLocalstackIfNotRunning(): LocalStackContainer? {
    if (localstackIsRunning()) return null
    log.info("Starting localstack test container")
    val logConsumer = Slf4jLogConsumer(log).withPrefix("localstack")
    return LocalStackContainer(
      DockerImageName.parse("localstack/localstack").withTag("3.8"),
    ).apply {
      withServices(LocalStackContainer.Service.SQS, LocalStackContainer.Service.SNS)
      withEnv("EAGER_SERVICE_LOADING", "1")
      withEnv("DISABLE_EVENTS", "1")
      withEnv("DNS_ADDRESS", "0")
      withReuse(false)
      waitingFor(
        Wait.forLogMessage(".*Ready.*", 1)
          .withStartupTimeout(java.time.Duration.ofSeconds(120))
      )
      start()
      followOutput(logConsumer)
      log.info("LocalStack container started successfully at {}", endpoint)
    }
  }

  private fun localstackIsRunning(): Boolean = try {
    val serverSocket = ServerSocket(4566)
    serverSocket.close()
    false
  } catch (e: IOException) {
    log.info("localstack already running on port 4566")
    true
  }
}

