package uk.gov.justice.digital.hmpps.managingprisonerappsapi.sqlgenerator

import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime

/*
 * AppTypeDepartmentMapper.kt
 * This app is used to update the application_type active flag and to update the department_slug in establishment_application_type table.
 *
 * Steps
 * 1. Run the sql specified in Departments tab of src/main/resources/dataconfig/xlsx/Apptype-Department-Mapper.xlsx file on live
 *    DB and export the data as .csv
 * 2. Open the .csv file  and save as xlsx file.
 * 3. Copy the contents of the above .csv file into the src/main/resources/dataconfig/xlsx/Apptype-Department-Mapper.xlsx
 * 4. Update the active flag and department as necessary.
 * 5. Filter the establishment column. Currently this will generate sql's for each establishment
 * 4. All the rows that are being updated should set the 'updated' column to 'Yes' to prevent unnecessary updated to DB
 * 5. Run the program AppTypeDepartmentMapper.kt
 * 6. The output will be in the folder 'output/${ESTABLISHMENT}_establishment-department-mapping.sql'
 *
 * The full set of sql will be wrapped in a transaction and will be rolled back by default.
 * The output data that will be inserted into DB will be output when you run the sql using DBeaver 'Execute Sql Script' button.
 * For actual running replace the 'rollback' statement with 'commit' and run the sql in any database client.
 */

private const val ESTABLISHMENT = "CKI"

private const val MAPPING_FILE_PATH = "dataconfig/xlsx/Apptype-Department-Mapper.xlsx"
private const val OUTPUT_DIR_PATH = "output/sqlgenerator"
private const val SHEET_INDEX = 0

var allAppTypeDepartmentMappings: List<AppTypeDepartmentMappingConfig> = emptyList()
var establishmentAppTypeDepartmentMappings: List<AppTypeDepartmentMappingConfig> = emptyList()

private val logger = LoggerFactory.getLogger("AppTypeDepartmentMapper")

fun main(args: Array<String>) {
  allAppTypeDepartmentMappings = loadAppTypeDepartmentMappings(MAPPING_FILE_PATH, SHEET_INDEX)
  logger.info("Loaded {} mappings from {}", allAppTypeDepartmentMappings.size, MAPPING_FILE_PATH)

  establishmentAppTypeDepartmentMappings = filterMappingsByEstablishment(ESTABLISHMENT)
  logger.info("To Update {} mappings in establishment {}", establishmentAppTypeDepartmentMappings.size, ESTABLISHMENT)

  generateSqlForEstablishmentMappings(establishmentAppTypeDepartmentMappings)
}

private fun generateSqlForEstablishmentMappings(mappings: List<AppTypeDepartmentMappingConfig>) {
  val sqlStatements = mutableListOf<String>()
  val localDateTime = LocalDateTime.now()
  var appTypeList = mappings.map { it.appTypeId }.distinct().sorted()
  sqlStatements.add("begin;")
  sqlStatements.add("")
  for (mapping in mappings) {
    val sql = """
            update establishment_application_type
            set active = ${if (mapping.active) "true" else "false"}, department_slug = '${mapping.departmentSlug}', last_modified_date = '$localDateTime'
            where establishment_id = ${if (mapping.establishmentId != null) "'${mapping.establishmentId}'" else "NULL"} and application_type_id = ${mapping.appTypeId};          
    """.trimIndent()
    sqlStatements.add(sql)
    sqlStatements.add("")

    val sql1 = """
            update establishment_departments set active = false where  department_id = 
            (select id from department d where d.slug = '${mapping.departmentSlug}') 
            and  establishment_id = '$ESTABLISHMENT'; 
    """.trimIndent()
    sqlStatements.add(sql1)
    sqlStatements.add("")
  }

  val sql4 = """
            --title: Active Departments
            select ed.establishment_id, d.name, ed.active from establishment_departments ed 
            inner join department d on d.id =ed.department_id 
            where ed.establishment_id ='$ESTABLISHMENT' and ed.active =true
            order by d.name;
  """.trimIndent()
  sqlStatements.add(sql4)
  sqlStatements.add("")

  val sql2 = """
            --title: Active App Types
            select ag.name, at.name, eat.active, eat.department_slug  from establishment_application_type eat 
            inner join application_type at on at.id =eat.application_type_id 
            inner join application_group ag on ag.id=at.application_group_id 
            where eat.establishment_id ='$ESTABLISHMENT' and eat.active =true
            order by ag."name", at."name";
  """.trimIndent()
  sqlStatements.add(sql2)
  sqlStatements.add("")

  val sql3 = """
            --title: Updated Rows
            select eat.establishment_id, ag.name, at.name, eat.active, eat.application_type_id, eat.department_slug  from establishment_application_type eat 
            inner join application_type at on at.id =eat.application_type_id 
            inner join application_group ag on ag.id=at.application_group_id 
            where eat.establishment_id ='$ESTABLISHMENT' and eat.application_type_id in (${appTypeList.joinToString(", ")})
            order by ag."name", at."name";
  """.trimIndent()
  sqlStatements.add(sql3)
  sqlStatements.add("")

  sqlStatements.add("rollback;")

  // Write SQL statements to a file
  val outputDir = File(OUTPUT_DIR_PATH).apply { mkdirs() }
  val file = File(outputDir, "${ESTABLISHMENT}_establishment-department-mapping.sql")
  file.writeText(sqlStatements.joinToString("\n"))
  logger.info("SQL written to: {}", file.absolutePath)
}

private fun filterMappingsByEstablishment(establishmentId: String): List<AppTypeDepartmentMappingConfig> = allAppTypeDepartmentMappings.filter {
  it.establishmentId.equals(
    establishmentId,
    ignoreCase = true,
  ) &&
    it.updated.equals("Yes", ignoreCase = true)
}

private fun loadAppTypeDepartmentMappings(
  filePath: String,
  sheetIndex: Int,
): List<AppTypeDepartmentMappingConfig> {
  val formatter = DataFormatter()
  openMappingWorkbook(filePath).use { workbook ->
    val sheet = workbook.getSheetAt(sheetIndex)
    val headerRow = sheet.getRow(sheet.firstRowNum)
      ?: error("Sheet ${sheet.sheetName} does not have a header row")

    val headers = buildMap {
      for (cellIndex in 0 until headerRow.lastCellNum) {
        val header = formatter.formatCellValue(headerRow.getCell(cellIndex))
          .normalizeHeader()
        if (header.isNotEmpty()) {
          put(header, cellIndex)
        }
      }
    }

    fun value(rowIndex: Int, keyAliases: List<String>): String {
      val row = sheet.getRow(rowIndex) ?: return ""
      val index = keyAliases
        .asSequence()
        .map { it.normalizeHeader() }
        .firstNotNullOfOrNull { headers[it] } ?: return ""
      return formatter.formatCellValue(row.getCell(index)).trim()
    }

    val mappings = mutableListOf<AppTypeDepartmentMappingConfig>()
    for (rowIndex in (sheet.firstRowNum + 1)..sheet.lastRowNum) {
      val appTypeIdRaw = value(rowIndex, listOf("apptypeid", "applicationtypeid"))
      if (appTypeIdRaw.isBlank()) continue

      val establishmentId = value(rowIndex, listOf("establishmentid", "establishment"))
      val appType = value(rowIndex, listOf("apptype", "applicationtype"))
      val active = value(rowIndex, listOf("active")).let {
        it.equals("true", ignoreCase = true) || it == "1" || it.equals("yes", ignoreCase = true)
      }
      val departmentSlug = value(rowIndex, listOf("departmentslug", "slug"))
      val updated = value(rowIndex, listOf("updated"))

      mappings += AppTypeDepartmentMappingConfig(
        establishmentId = establishmentId,
        appType = appType,
        appTypeId = appTypeIdRaw.toDouble().toInt(),
        active = active,
        departmentSlug = departmentSlug,
        updated = updated,
      )
    }
    return mappings
  }
}

private fun openMappingWorkbook(filePath: String) = Thread.currentThread().contextClassLoader?.getResourceAsStream(filePath)?.use { WorkbookFactory.create(it) }
  ?: run {
    val directFile = File(filePath)
    val sourceFile = File("src/main/resources/$filePath")
    val mappingFile = when {
      directFile.exists() -> directFile
      sourceFile.exists() -> sourceFile
      else -> {
        logger.error("Mapping file not found. Checked classpath resource '$filePath', '${directFile.absolutePath}', '${sourceFile.absolutePath}'")
        throw IllegalArgumentException("Mapping file not found: $filePath")
      }
    }
    WorkbookFactory.create(mappingFile)
  }

data class AppTypeDepartmentMappingConfig(
  val establishmentId: String = "",
  val appType: String = "",
  val appTypeId: Int,
  val active: Boolean = false,
  val departmentSlug: String = "",
  val updated: String = "",
)

private fun String.normalizeHeader(): String = trim()
  .lowercase()
  .replace(Regex("[^a-z0-9]"), "")
