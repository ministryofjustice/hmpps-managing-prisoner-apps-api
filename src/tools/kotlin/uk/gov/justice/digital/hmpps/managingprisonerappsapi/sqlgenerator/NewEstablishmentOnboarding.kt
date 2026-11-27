package uk.gov.justice.digital.hmpps.managingprisonerappsapi.sqlgenerator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.uuid.Generators
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.DriverManager
import java.sql.ResultSet
import java.time.LocalDateTime

/*
## Steps to generate sql for Onboarding new Prison
1. Set the ONBOARD_ESTABLISHMENT_ID
2. Create a copy of the src/main/resources/dataconfig/Ref-Data.yaml and name the file as {ONBOARD_ESTABLISHMENT_ID}.yaml
3. Modify the yaml to exclude 'applicationGroups', application_types and departments not required for this establishment
4. Run NewEstablishmentOnboarding.kt

The sql will be generated in the output folder and will be in format Onboarding-ESTABLISHMENT_ID.sql
The full set of sql will be wrapped in a transaction and will be rolled back by default.
The output data that will be inserted into DB will be output when you run the sql using DBeaver 'Execute Sql Script' button.
For actual running replace the 'rollback' statement with 'commit' and run the sql in any database client.
*/

// ************************************************
private const val ONBOARD_ESTABLISHMENT_ID = "CKIX"
// ************************************************

private val logger = LoggerFactory.getLogger("NewEstablishmentGenerator.kt")
private const val OUTPUT_DIR_PATH = "output/sqlgenerator"

data class EstablishmentConfig(
  val NEW_ESTABLISHMENT_ID: String = "",
  val NEW_ESTABLISHMENT_NAME: String = "",

  val excludedApplicationTypes: List<Long>? = emptyList(),
  val excludedApplicationGroups: List<String>? = emptyList(),
  var excludedDepartments: List<String>? = emptyList(),
  var departmentsList: List<String> = emptyList(),
)

data class ApplicationGroupTypeRow(val appGroupName: String, val appTypeId: Long, val appTypeName: String)
data class Department(val id: String, val name: String)

// Loaded from YAML at startup
lateinit var config: EstablishmentConfig
val ESTABLISHMENT_NAME get() = config.NEW_ESTABLISHMENT_NAME
val ESTABLISHMENT_ID get() = config.NEW_ESTABLISHMENT_ID
private val sqlStatements = mutableListOf<String>()

fun main(args: Array<String>) {
  config = loadConfig(ONBOARD_ESTABLISHMENT_ID)
  logger.info("Loaded config for: ${ESTABLISHMENT_NAME} (${ESTABLISHMENT_ID})")

  createEstablishment()
  createEstablishmentApplicationTypeSqlForNewEstablishment()
  addToEstablishmentDepartments()
  validateDataSql()
}

private fun createEstablishment() {
  var exists: Boolean = false
  sqlStatements.add("begin;")
  logger.info("Check data for ${ESTABLISHMENT_NAME}")
  val sql = "select * from establishment where id = '${ESTABLISHMENT_ID}'"
  invokeDB(sql) { rs ->
    while (rs.next()) {
      logger.debug("${ESTABLISHMENT_NAME} already exists")
      exists = true
    }
  }
  if (!exists) {
    logger.info("Creating establishment data for ${ESTABLISHMENT_NAME}")
    val sql = """
        INSERT INTO establishment (id, name, default_departments) VALUES ('$ESTABLISHMENT_ID', '$ESTABLISHMENT_NAME', false);
    """.trimIndent()
    logger.debug(sql)
    sqlStatements.add(sql)
    sqlStatements.add("-- title: $ESTABLISHMENT_NAME")
    sqlStatements.add("select * from establishment where id = '$ESTABLISHMENT_ID';")
    sqlStatements.add("")
  } else {
    logger.info("Establishment ${ESTABLISHMENT_NAME} already exists, Skipping")
  }
}

/*
Insert data into establishment_departments
 */
private fun addToEstablishmentDepartments() {
  val excludedDepartments = config.excludedDepartments
  val departmentIdsInDB = loadEstablishmentDepartmentFromDB()
  val allDepartments = loadAllDepartmentsFromDB()
  sqlStatements.add("")
  logger.info("Adding to Establishment_Department")
  allDepartments.forEach { department ->
    var active: Boolean = true
    if (!departmentIdsInDB.contains(department.name)) {
      if (excludedDepartments?.contains(department.name) == true) {
        active = false
      }
      val uuid: String? = generateUUID()
      logger.debug("create Department ${department.name} in establishment_departments")
      val sql = """
                insert into establishment_departments (id, department_id, establishment_id, active) 
                values ('$uuid', '${department.id}', '$ESTABLISHMENT_ID', $active);
      """.trimIndent()
      sqlStatements.add(sql)
    } else {
      logger.debug("Department ${department.name} already exist in establishment_departments")
    }
  }
  logger.info("Adding to Establishment_Department - Complete")
  sqlStatements.add("")
}

private fun createEstablishmentApplicationTypeSqlForNewEstablishment() {
  val appTypesAndGroupsDB = loadAppTypesAndGroupsFromDB()
  logger.info("Generating sqls for 'establishment_application_type' for ${ESTABLISHMENT_NAME}")
  val existingIdsInDb = loadEstabishmentApplicationTypesFromDB()

  appTypesAndGroupsDB.forEach { dbDataRow ->
    val uuid: String? = generateUUID()
    val localDateTime = LocalDateTime.now()
    var active: Boolean = false

    // Is it already in DB ? Then skip it
    if (!existingIdsInDb.contains(dbDataRow.appTypeId)) {
      if (config.excludedApplicationGroups?.contains(dbDataRow.appGroupName) != true &&
        config.excludedApplicationTypes?.contains(dbDataRow.appTypeId) != true
      ) {
        active = true
      }
      val sql = """
                insert into establishment_application_type (id, establishment_id, application_type_id, active, created_by, created_date, last_modified_by, last_modified_date)
                values('$uuid', '$ESTABLISHMENT_ID', ${dbDataRow.appTypeId}, $active, 'BOBYJDEV', '$localDateTime', 'BOBYJDEV', '$localDateTime');
      """.trimIndent()
      sqlStatements.add(sql)
      logger.debug("Adding SQL for ${dbDataRow.appTypeName} - $active")
    } else {
      logger.debug("ApplicationTypeId ${dbDataRow.appTypeName} already existin DB, skippig")
    }
  }
  logger.info("Generating sqls for 'establishment_application_type' - Complete")
  sqlStatements.add("")
}

private fun loadEstabishmentApplicationTypesFromDB(): List<Long> {
  var eaTypes = mutableListOf<Long>()
  var sql = """
        select application_type_id from establishment_application_type where establishment_id= '${ESTABLISHMENT_ID}' order by id;
  """.trimIndent()
  logger.debug("Fetching establishment_application_types from DB")
  invokeDB(sql) { rs ->
    while (rs.next()) {
      eaTypes.add(rs.getLong("application_type_id"))
    }
  }
  return eaTypes
}

/**
 * Load all departments - excluded ones for this establishment
 */
private fun loadAllDepartmentsFromDB(): List<Department> {
  val allDepartments = mutableListOf<Department>()
  var sql = """
        select id, name from department order by name;
  """.trimIndent()
  logger.debug("Fetching departments from DB")
  val results = mutableListOf<String>()
  invokeDB(sql) { rs ->
    while (rs.next()) {
      val department = Department(
        name = rs.getString("name"),
        id = rs.getString("id"),
      )
      allDepartments.add(department)
    }
  }
  return allDepartments
}

private fun loadEstablishmentDepartmentFromDB(): List<String> {
  val sql = """
        select department_id from establishment_departments where establishment_id='${ESTABLISHMENT_ID}'; 
  """.trimIndent()
  logger.debug("Fetching establishment_departments from DB")
  val results = mutableListOf<String>()
  invokeDB(sql) { rs ->
    while (rs.next()) {
      results.add(rs.getString("department_id"))
    }
  }
  return results
}

private fun validateDataSql() {
  sqlStatements.add("")
  val sql = """
        --title: EAT Inserted
        select
        (select count(*) from establishment_application_type where establishment_id='$ESTABLISHMENT_ID' and active =true) as Active,
        (select count(*) Inactive from establishment_application_type where establishment_id='$ESTABLISHMENT_ID' and active =false) as Inactive,
        (select count(*) from establishment_application_type where establishment_id='$ESTABLISHMENT_ID') as Total,
        (select count(*) from application_type) as MasterData;
  """.trimIndent()
  sqlStatements.add(sql)
  sqlStatements.add("")

  sqlStatements.add("--title: Department Count")
  sqlStatements.add("select count(*) from establishment_departments where establishment_id='${ESTABLISHMENT_ID}';")
  sqlStatements.add("--title: Departments")
  sqlStatements.add(
    "select d.name, ed.active from establishment_departments ed " +
      "inner join department d on d.id =ed.department_id where ed. establishment_id='${ESTABLISHMENT_ID}' order by ed.active desc,d.name;",
  )

  sqlStatements.add("")
  sqlStatements.add("rollback;")
  sqlStatements.add("")
  writeToFile()
}

private fun loadAppTypesAndGroupsFromDB(): List<ApplicationGroupTypeRow> {
  var sql = """
        select ag.name appGroupName,t.id appTypeId, t.name appTypeName from application_group ag 
        inner join application_type t on t.application_group_id =ag.id 
        order by ag.name
  """.trimIndent()
  logger.debug("Fetching Application types from DB ")
  val results = mutableListOf<ApplicationGroupTypeRow>()
  invokeDB(sql) { rs ->
    while (rs.next()) {
      results.add(
        ApplicationGroupTypeRow(
          appGroupName = rs.getString("appGroupName"),
          appTypeId = rs.getLong("appTypeId"),
          appTypeName = rs.getString("appTypeName"),
        ),
      )
    }
  }
  logger.debug("Fetched ${results.size} application types from DB")
  return results
}

fun generateUUID(): String? = Generators.timeBasedEpochGenerator().generate().toString()

fun invokeDB(sql: String, process: (ResultSet) -> Unit) {
  val dbEndpoint = getEnv("DB_ENDPOINT", "localhost:5432")
  val dbName = getEnv("DB_NAME", "managingapps")
  val dbUser = getEnv("DB_USERNAME", "postgres")
  val dbPassword = getEnv("DB_PASSWORD", "password")
  val url = "jdbc:postgresql://$dbEndpoint/$dbName"

  logger.debug("Connecting to $url as $dbUser")
  DriverManager.getConnection(url, dbUser, dbPassword).use { conn ->
    conn.createStatement().use { stmt ->
      stmt.executeQuery(sql).use { rs ->
        process(rs)
      }
    }
  }
}

/** ─── Env loader ───────────────────────
 * Set NEW_ESTABLISHMENT_ID in dev.env
 *
 */
private val env: Map<String, String> by lazy {
  val envMap = System.getenv().toMutableMap()
  val devEnvFile = File("dev.env")
  if (devEnvFile.exists()) {
    devEnvFile.readLines()
      .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
      .forEach { line ->
        val (key, value) = line.split("=", limit = 2)
        envMap[key.trim()] = value.trim()
      }
    logger.debug("Loaded dev.env from: ${devEnvFile.absolutePath}")
  } else {
    logger.warn("dev.env not found at: ${devEnvFile.absolutePath}")
  }
  envMap
}

fun getEnv(key: String, default: String? = null): String? = env[key] ?: default

fun loadConfig(establishmentId: String): EstablishmentConfig {
  val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
  val classpathPath = "dataconfig/$establishmentId.yaml"

  Thread.currentThread().contextClassLoader?.getResourceAsStream(classpathPath)?.use {
    return mapper.readValue<EstablishmentConfig>(it)
  }

  val directFile = File(classpathPath)
  if (directFile.exists()) {
    return mapper.readValue<EstablishmentConfig>(directFile)
  }

  val sourceFile = File("src/main/resources/$classpathPath")
  if (sourceFile.exists()) {
    return mapper.readValue<EstablishmentConfig>(sourceFile)
  }

  logger.error(
    "Config file not found. Checked classpath resource '{}', '{}', '{}'",
    classpathPath,
    directFile.absolutePath,
    sourceFile.absolutePath,
  )
  throw IllegalArgumentException("Config file not found: $classpathPath")
}

fun writeToFile() {
  val outputDir = File(OUTPUT_DIR_PATH).apply { mkdirs() }
  val file = File(outputDir, "Onboarding-${ESTABLISHMENT_ID}.sql")
  file.writeText(sqlStatements.joinToString("\n"))
  logger.info("SQL written to: ${file.absolutePath}")
}
