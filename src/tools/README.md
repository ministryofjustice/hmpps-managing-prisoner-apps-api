# NewEstablishmentOnboarding.kt
# =============================
This app is used to generate sql for onboarding a new prison.

## Steps to generate sql for Onboarding new Prison
1. Set the ONBOARD_ESTABLISHMENT_ID
2. Create a copy of the src/main/resources/dataconfig/Ref-Data.yaml and name the file as {ONBOARD_ESTABLISHMENT_ID}.yaml
3. Modify the yaml to exclude 'applicationGroups', application_types and departments not required for this establishment
4. Run NewEstablishmentOnboarding.kt

The sql will be generated in the output folder and will be in format Onboarding-ESTABLISHMENT_ID.sql
The full set of sql will be wrapped in a transaction and will be rolled back by default.
The output data that will be inserted into DB will be output when you run the sql using DBeaver 'Execute Sql Script' button.
For actual running replace the 'rollback' statement with 'commit' and run the sql in any database client.
# =================================================================================================================================================

# AppTypeDepartmentMapper.kt
This app is used to update the application_type active flag and to update the department_slug in establishment_application_type table.

## Steps
1. Run the sql specified in Departments tab of src/main/resources/dataconfig/xlsx/Apptype-Department-Mapper.xlsx file on live 
   DB and export the data as .csv
2. Open the .csv file  and save as xlsx file.
3. Copy the contents of the above .csv file into the src/main/resources/dataconfig/xlsx/Apptype-Department-Mapper.xlsx
4. Update the active flag and department as necessary.
5. Filter the establishment column. Currently this will generate sql's for each establishment
4. All the rows that are being updated should set the 'updated' column to 'Yes' to prevent unnecessary updated to DB
5. Run the program AppTypeDepartmentMapper.kt
6. The output will be in the folder 'output/${ESTABLISHMENT}_establishment-department-mapping.sql'

The full set of sql will be wrapped in a transaction and will be rolled back by default.
The output data that will be inserted into DB will be output when you run the sql using DBeaver 'Execute Sql Script' button.
For actual running replace the 'rollback' statement with 'commit' and run the sql in any database client.
# =================================================================================================================================================

# Note
To keep the master data in sync, run the sql's in all environments.
