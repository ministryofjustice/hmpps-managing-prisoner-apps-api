package uk.gov.justice.digital.hmpps.managingprisonerappsapi.config

// @Configuration
class InitializeDB {
  // This only for populating database in dev till flyway is not used. It will be deleted after integrating flyway.
  // Data will be populated in dev through flyway sql script.
  /*@Bean
  fun initializeDatabase(datasource: DataSource) {
    val dataSourceInitializer = DataSourceInitializer()
    dataSourceInitializer.setDataSource(datasource)
    dataSourceInitializer.setDatabasePopulator(DatabasePopulator {  })
  }*/
  /*private val establishmentIdFirst = "TEST_ESTABLISHMENT_FIRST"
  private val establishmentIdSecond = "TEST_ESTABLISHMENT_SECOND"
  private val establishmentIdThird = "TEST_ESTABLISHMENT_THIRD"
  private val assignedGroupFirst = UUID.fromString("343a7876-07b9-4ef8-947c-7cf554fae864")
  private val assignedGroupFirstName = "Business Hub"
  private val assignedGroupSecond = UUID.fromString("1ebf7110-82cf-4dc1-b872-92c7d71847fc")
  private val assignedGroupSecondName = "OMU"
  private val requestedByFirst = "A12345"
  private val requestedByFirstMainName = "John"
  private val requestedByFirstSurname = "Smith"
  private val requestedBySecondMainName = "John"
  private val requestedBySecondSurname = "Butler"
  private val requestedBySecond = "B12345"
  private val requestedByThird = "C12345"
  private val requestedByThirdMainName = "Test"
  val requestedByThirdSurname = "User"

  @Bean("establishment")
  @Profile("dev")
  fun establishmentRepository(establishmentRepository: EstablishmentRepository): EstablishmentRepository {
    establishmentRepository.save(Establishment(establishmentIdFirst, "ESTABLISHMENT_NAME_1"))
    return establishmentRepository
  }

  @Bean("groups")
  @Profile("dev")
  fun groupRepository(groupRepository: GroupRepository): GroupRepository {
    groupRepository.save(
      generateGroups(
        assignedGroupFirst,
        establishmentIdFirst,
        assignedGroupFirstName,
        listOf(AppType.PIN_PHONE_ADD_NEW_CONTACT, AppType.PIN_PHONE_REMOVE_CONTACT),
        GroupType.WING,
      ),
    )
    /*groupRepository.save(
        DataGenerator.generateGroups(
            assignedGroupFirst,
            establishmentIdFirst,
            assignedGroupFirstName,
            listOf(AppType.PIN_PHONE_ADD_NEW_CONTACT, AppType.PIN_PHONE_REMOVE_CONTACT),
            GroupType.WING
        )
    )*/
    groupRepository.save(
      generateGroups(
        assignedGroupSecond,
        establishmentIdFirst,
        assignedGroupSecondName,
        listOf(AppType.PIN_PHONE_ADD_NEW_CONTACT, AppType.PIN_PHONE_REMOVE_CONTACT, AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS, AppType.PIN_PHONE_EMERGENCY_CREDIT_TOP_UP),
        GroupType.WING,
      ),
    )
    return groupRepository
  }

  @Bean("app")
  @Profile("dev")
  fun appRepository(appRepository: AppRepository): AppRepository {
    appRepository.save(
      generateApp(
        UUID.fromString("a6d8de23-f900-41ed-8f0d-e65385d89795"),
        establishmentIdFirst,
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        requestedByFirst,
        LocalDateTime.now(ZoneOffset.UTC).minusDays(1),
        requestedByFirstMainName,
        requestedByFirstSurname,
        AppStatus.PENDING,
        assignedGroupFirst,
      ),
    )
    appRepository.save(
      generateApp(
        UUID.fromString("6bfc799d-6788-4785-b01d-2694435e2179"),
        establishmentIdFirst,
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        requestedByFirst,
        LocalDateTime.now(ZoneOffset.UTC).minusDays(2),
        requestedByFirstMainName,
        requestedByFirstSurname,
        AppStatus.PENDING,
        assignedGroupFirst,
      ),
    )
    appRepository.save(
      generateApp(
        UUID.fromString("638f2d8e-a999-4717-b532-bead1b20e4f8"),
        establishmentIdFirst,
        AppType.PIN_PHONE_ADD_NEW_CONTACT,
        requestedByFirst,
        LocalDateTime.now(ZoneOffset.UTC).minusDays(3),
        requestedByFirstMainName,
        requestedByFirstSurname,
        AppStatus.PENDING,
        assignedGroupFirst,
      ),
    )
    appRepository.save(
      generateApp(
        UUID.fromString("f39114ad-6f23-4cbe-83bf-8677f7cc9e7a"),
        establishmentIdFirst,
        AppType.PIN_PHONE_REMOVE_CONTACT,
        requestedBySecond,
        LocalDateTime.now(ZoneOffset.UTC).minusDays(4),
        requestedBySecondMainName,
        requestedBySecondSurname,
        AppStatus.PENDING,
        assignedGroupFirst,
      ),
    )
    appRepository.save(
      generateApp(
        UUID.fromString("f1cef96a-2b31-46d2-b7ff-539045cf6399"),
        establishmentIdSecond,
        AppType.PIN_PHONE_REMOVE_CONTACT,
        requestedByThird,
        LocalDateTime.now(ZoneOffset.UTC).minusDays(5),
        requestedByFirstMainName,
        requestedByFirstSurname,
        AppStatus.PENDING,
        assignedGroupFirst,
      ),
    )
    appRepository.save(
      generateApp(
        UUID.fromString("5e3ad954-460b-4037-ba64-6b7b47f39b7c"),
        establishmentIdSecond,
        AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS,
        requestedByThird,
        LocalDateTime.now(ZoneOffset.UTC).minusDays(6),
        requestedByThirdMainName,
        requestedByThirdSurname,
        AppStatus.PENDING,
        assignedGroupSecond,
      ),
    )
    appRepository.save(
      generateApp(
        UUID.fromString("24082de2-16e7-4d21-8d18-b1eeb9d6486e"),
        establishmentIdSecond,
        AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS,
        requestedByThird,
        LocalDateTime.now(ZoneOffset.UTC).minusDays(7),
        requestedByThirdMainName,
        requestedByThirdSurname,
        AppStatus.PENDING,
        assignedGroupSecond,
      ),
    )
    appRepository.save(
      generateApp(
        UUID.fromString("e1dc1afc-f94b-4181-87c4-3b04f6c03f54"),
        establishmentIdThird,
        AppType.PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS,
        requestedByThird,
        LocalDateTime.now(ZoneOffset.UTC).minusDays(8),
        requestedByThirdMainName,
        requestedByThirdSurname,
        AppStatus.PENDING,
        assignedGroupSecond,
      ),
    )
    return appRepository
  }

  fun generateApp(
    id: UUID,
    establishmentId: String,
    appType: AppType,
    requestedBy: String,
    requestedDate: LocalDateTime,
    requestedByFirstName: String,
    requestedByLastName: String,
    appStatus: AppStatus,
    groupId: UUID,
  ): App = App(
    UUID.randomUUID(),
    UUID.randomUUID().toString(),
    groupId,
    appType,
    requestedDate,
    LocalDateTime.now(ZoneOffset.UTC),
    "testStaaf@moj",
    LocalDateTime.now(ZoneOffset.UTC),
    "testStaaf@moj",
    arrayListOf(),
    mutableListOf(HashMap<String, Any>().apply { put("contact", 123456) }),
    requestedBy,
    requestedByFirstName,
    requestedByLastName,
    appStatus,
    establishmentId,
    mutableListOf(),
  )

  fun generateGroups(
    id: UUID,
    establishmentId: String,
    groupName: String,
    initialApps: List<AppType>,
    groupType: GroupType,
  ): Groups = Groups(
    id,
    groupName,
    establishmentId,
    initialApps,
    groupType,
  )*/
}
