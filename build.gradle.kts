plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "11.0.1"
  id("org.owasp.dependencycheck") version "12.2.2"
  kotlin("plugin.spring") version "2.4.10"
  kotlin("plugin.jpa") version "2.4.10"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:3.0.0")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("com.fasterxml.uuid:java-uuid-generator:5.2.0")
  implementation("org.flywaydb:flyway-core")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.postgresql:postgresql:42.7.12")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.11.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.11.0")

  implementation("org.flywaydb:flyway-database-postgresql")

  // SQS/SNS dependencies
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.4.0")
  testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:3.0.0")

  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("org.mockito.kotlin:mockito-kotlin:6.2.3")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:5.1.1")

  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.43") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("com.h2database:h2:2.4.240")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-subject-access-request-test-support:2.5.0") {
    exclude(group = "org.springframework.boot", module = "spring-boot-webtestclient")
  }

  // Localstack
  testImplementation("org.testcontainers:localstack:1.20.4")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.2")
}

kotlin {
  jvmToolchain(25)
  compilerOptions {
    freeCompilerArgs.addAll("-Xannotation-default-target=param-property")
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_25
  targetCompatibility = JavaVersion.VERSION_25
}

dependencyCheck {
  nvd.datafeedUrl = "file:///opt/vulnz/cache"
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
  }
}
