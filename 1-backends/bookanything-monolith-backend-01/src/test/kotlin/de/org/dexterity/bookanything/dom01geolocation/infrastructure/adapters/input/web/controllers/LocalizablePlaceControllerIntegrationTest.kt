package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.org.dexterity.bookanything.dom01geolocation.domain.events.LocalizablePlaceCreatedEvent
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateLocalizablePlaceRestRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.LocalizablePlaceRestResponse
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.entities.LocalizablePlaceElasticEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.LocalizablePlaceJpaEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.LocalizablePlaceJpaRepository
import de.org.dexterity.bookanything.shared.integrationtests.AbstractIntegrationTest
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.http.MediaType
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.File
import java.time.Duration
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = ["spring.kafka.consumer.group-id=test-group"])
class LocalizablePlaceControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var localizablePlaceJpaRepository: LocalizablePlaceJpaRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var elasticsearchOperations: ElasticsearchOperations

    private val acks: BlockingQueue<LocalizablePlaceCreatedEvent> = LinkedBlockingQueue()

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry

    @BeforeEach
    fun setup() {
        /*
        if (elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).exists()) {
            elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).delete()
        }
        elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).create()
        */
        elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).refresh()

    }

    @AfterEach
    fun tearDown() {
        localizablePlaceJpaRepository.deleteAll()
        elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).delete()
        // elasticsearchOperations.delete(DeleteQuery.builder(CriteriaQuery(Criteria.where("id").exists())).build(), LocalizablePlaceElasticEntity::class.java)
    }

    @KafkaListener(topics = ["localizable-place-created-topic"], groupId = "test-group")
    fun consume(message: LocalizablePlaceCreatedEvent) {
        acks.add(message)
    }

    @Test
    fun contextLoads() {
    }

    @Test
    fun shouldCreateANewLocalizablePlace() {
        val requestBody = """
            {
                "friendlyId": "cd-teste",
                "name": "CD Teste",
                "latitude": -23.55052,
                "longitude": -46.633308
            }
        """

        val result = mockMvc.post("/api/v1/localizable-places") {
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
            with(jwt())
        }.andExpect { status { isCreated() } }
            .andReturn()

        val responseBody = result.response.contentAsString
        assertNotNull(responseBody)

        val savedCenters = localizablePlaceJpaRepository.findAll()
        assertEquals(1, savedCenters.size)
        assertEquals("CD Teste", savedCenters[0].name)
        assertEquals("cd-teste", savedCenters[0].friendlyId)
    }

    @Test
    fun shouldPublishOneLocalizablePlaceCreateEventToKafka() {
        val requestBody = """
            {
                "friendlyId": "cd-kafka-teste",
                "name": "CD Kafka Teste",
                "latitude": -20.0,
                "longitude": -40.0
            }
        """

        val createResult = mockMvc.post("/api/v1/localizable-places") {
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
            with(jwt())
        }.andExpect { status { isCreated() } }
         .andReturn()

        val createdResponse = objectMapper.readValue<LocalizablePlaceRestResponse>(createResult.response.contentAsString)
        assertNotNull(createdResponse.id)
        assertEquals("CD Kafka Teste", createdResponse.name)
        assertEquals("cd-kafka-teste", createdResponse.friendlyId)

        // Wait for Kafka and Elasticsearch to process the events
        await().atMost(Duration.ofSeconds(30)).untilAsserted {

            elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).refresh()

            val query = CriteriaQuery(Criteria.where("id").exists())
            val searchHits = elasticsearchOperations.search(query, LocalizablePlaceElasticEntity::class.java)
            val count = searchHits.totalHits
            if (count < 1) {
                val foundIds = searchHits.map { it.content.id }.joinToString()
                println("Documents found in ElasticSearch: $foundIds")
            }
            assertEquals(1, count)
        }

        // 2. Retrieve by ID
        val findByIdResult = mockMvc.get("/api/v1/localizable-places/${createdResponse.id}") {
            with(jwt())
        }.andExpect { status { isOk() } }.andReturn()

        val foundByIdResponse = objectMapper.readValue<LocalizablePlaceRestResponse>(findByIdResult.response.contentAsString)
        assertEquals(createdResponse.id, foundByIdResponse.id)
        assertEquals("CD Kafka Teste", foundByIdResponse.name)


        val event = acks.poll(10, TimeUnit.SECONDS)
        assertNotNull(event, "Kafka message should have been received")
        assertEquals("CD Kafka Teste", event!!.name)
        assertEquals(-20.0, event.latitude)
        assertEquals(-40.0, event.longitude)
    }

    @Test
    fun shouldDeleteALocalizablePlaceById() {
        // Given
        val request = CreateLocalizablePlaceRestRequest(friendlyId = "to-delete", name = "LocalizablePlace To Delete", latitude = -23.55051, longitude = -46.633307)
        val result = createDistributionCenter(request)
        val response = objectMapper.readValue(result.response.contentAsString, LocalizablePlaceRestResponse::class.java)

        // When
        mockMvc.delete("/api/v1/localizable-places/{id}", response.id) {
            with(jwt())
        }.andExpect { status { isNoContent() } }

        elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).refresh()

        // When
        val resultFromGetById = mockMvc.get("/api/v1/localizable-places/{id}", response.id) {
            with(jwt())
        }.andExpect { status { isNotFound() } }
            .andReturn()

    }

    @Test
    fun shouldDeleteAllLocalizablePlaces() {

        val newItemsToCreateCount : Int = 10

        // Given
        IntRange(0, (newItemsToCreateCount - 1)).forEach { i ->
            createDistributionCenter(
                CreateLocalizablePlaceRestRequest(
                    friendlyId = "new-place-$i",
                    name = "One New LocalizablePlace - No.: ${i}",
                    latitude = Random.nextDouble(-23.55999, -23.55000 ),
                    longitude = Random.nextDouble(-46.633999, -46.633000)
                )
            )
        }

        // Wait for Kafka and Elasticsearch to process the events
        await().atMost(Duration.ofSeconds(30)).untilAsserted {

            elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).refresh()

            // When
            val result1 = mockMvc.get("/api/v1/localizable-places/all") {
                with(jwt())
            }.andExpect { status { isOk() } }
                .andReturn()

            // Then
            val responseBody1 = result1.response.contentAsString
            val centrosProximos1 = objectMapper.readValue(responseBody1, Array<LocalizablePlaceRestResponse>::class.java).toList()

            val count = centrosProximos1.size
            if (count < newItemsToCreateCount) {
                val foundIds = centrosProximos1.stream().map { it.id.toString() }.toList().joinToString()
                println("Documents found in ElasticSearch: $foundIds")
            }

            assertEquals(newItemsToCreateCount, (centrosProximos1.size))

        }

        // When
        mockMvc.delete("/api/v1/localizable-places/all") {
            with(jwt())
        }.andExpect { status { isNoContent() } }

        // Wait for Kafka and Elasticsearch to process the events
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            // When
            val result2 = mockMvc.get("/api/v1/localizable-places/all") {
                with(jwt())
            }.andExpect { status { isOk() } }
                .andReturn()

            // Then
            val responseBody2 = result2.response.contentAsString
            val centrosProximos2 =
                objectMapper.readValue(responseBody2, Array<LocalizablePlaceRestResponse>::class.java).toList()

            assertEquals(0, centrosProximos2.size)
        }
    }

    @Test
    fun shouldFindLocalizablePlacesWithinAGivenRadius() {
        // Wait until the consumer for the topic is assigned partitions
        await().atMost(Duration.ofSeconds(10)).until {
            kafkaListenerEndpointRegistry.listenerContainers.any { container ->
                container.groupId == "elasticsearch-indexer" && container.assignedPartitions?.isNotEmpty() == true
            }
        }

        // Given
        createDistributionCenter(CreateLocalizablePlaceRestRequest(friendlyId = "proximo-0", name = "CD Proximo 0", latitude = -23.55051, longitude = -46.633307))
        createDistributionCenter(CreateLocalizablePlaceRestRequest(friendlyId = "proximo-1", name = "CD Proximo 1", latitude = -23.55052, longitude = -46.633308))
        createDistributionCenter(CreateLocalizablePlaceRestRequest(friendlyId = "proximo-2", name = "CD Proximo 2", latitude = -23.55053, longitude = -46.633309))
        createDistributionCenter(CreateLocalizablePlaceRestRequest(friendlyId = "longe", name = "CD Longe", latitude = -22.9068, longitude = -43.1729))

        // Wait for Kafka and Elasticsearch to process the events
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            val query = CriteriaQuery(Criteria.where("id").exists())
            val searchHits = elasticsearchOperations.search(query, LocalizablePlaceElasticEntity::class.java)
            val count = searchHits.totalHits
            if (count < 4) {
                val foundIds = searchHits.map { it.content.id }.joinToString()
                println("Documents found in ElasticSearch: $foundIds")
            }
            assertEquals(4, count)
        }

        val targetLatitude = -23.55052
        val targetLongitude = -46.633308
        val radiusInKm = 1.0 // 1 km

        // When
        val result = mockMvc.get("/api/v1/localizable-places/search-nearby") {
            param("latitude", targetLatitude.toString())
            param("longitude", targetLongitude.toString())
            param("raioEmKm", radiusInKm.toString())
            with(jwt())
        }.andExpect { status { isOk() } }
            .andReturn()

        // Then
        val responseBody = result.response.contentAsString
        val centrosProximos = objectMapper.readValue(responseBody, Array<LocalizablePlaceRestResponse>::class.java).toList()

        assertEquals(3, centrosProximos.size)
        assert(centrosProximos.any { it.name == "CD Proximo 0" })
        assert(centrosProximos.any { it.name == "CD Proximo 1" })
        assert(centrosProximos.any { it.name == "CD Proximo 2" })
        assert(!centrosProximos.any { it.name == "CD Longe" })
    }

    @Test
    fun shouldSynchronizeDataFromDatabaseToElasticSearch() {
        val geometryFactory = GeometryFactory()

        val newItemsToCreateCount : Int = 100

        IntRange(0, (newItemsToCreateCount - 1)).forEach {
            val centroDistribuicao = LocalizablePlaceJpaEntity(
                id = UUID.randomUUID(),
                friendlyId = "new-place-$it",
                name = "One New LocalizablePlace - No.: ${it}",
                locationPoint = geometryFactory.createPoint(
                    Coordinate(
                        Random(-23).nextDouble(-23.55999, -23.55000 ),
                        Random(-46).nextDouble(-46.633999, -46.633000)
                    )
                )
            )
            localizablePlaceJpaRepository.saveAndFlush(centroDistribuicao)
        }


        mockMvc.post("/api/v1/localizable-places/synchronize") {
            with(jwt())
        }.andExpect { status { isOk() } }

        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            val searchHits = elasticsearchOperations.search(
                CriteriaQuery(Criteria.where("name").`is`("One New LocalizablePlace - No.:")),
                LocalizablePlaceElasticEntity::class.java
            )
            assertEquals(newItemsToCreateCount, searchHits.totalHits.toInt())
        }
    }

    @Test
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun shouldUploadAndProcessGeoJSONFileSuccessfully() {

        if (elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).exists()) {
            elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).delete()
        }
        elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).create()
        elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).refresh()

        val testGeoJSONFile : File = File("src/test/resources/geojson-data/test-electric-vehicle-charging-stations.geojson")
        // Given
        val sampleFile = MockMultipartFile(
            "file",
            "ev-charging-stations.geojson",
            MediaType.APPLICATION_JSON_VALUE,
            testGeoJSONFile.inputStream()
        )
        val newItemsToCreateCount : Int = 5

        // When
        mockMvc.perform(multipart("/api/v1/localizable-places/upload-geojson")
            .file(sampleFile)
            .param("contentDataType", "ev-charging-stations")
            .contentType(MediaType.MULTIPART_FORM_DATA)
        ).andExpect(status().isOk())

        // Wait for Kafka and Elasticsearch to process the events
        await().atMost(Duration.ofSeconds(30)).untilAsserted {

            elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).refresh()

            // When
            val result1 = mockMvc.get("/api/v1/localizable-places/all") {
                with(jwt())
            }.andExpect { status { isOk() } }
                .andReturn()

            // Then
            val responseBody1 = result1.response.contentAsString
            val centrosProximos1 = objectMapper.readValue(responseBody1, Array<LocalizablePlaceRestResponse>::class.java).toList()

            val count = centrosProximos1.size
            if (count < newItemsToCreateCount) {
                val foundIds = centrosProximos1.stream().map { it.id.toString() }.toList().joinToString()
                println("Documents found in ElasticSearch: $foundIds")
            }

            assertEquals(newItemsToCreateCount, (centrosProximos1.size))

        }

    }


    private fun createDistributionCenter(request: CreateLocalizablePlaceRestRequest): MvcResult {

        val mockMvcResult = mockMvc.post("/api/v1/localizable-places") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            with(jwt())
        }.andExpect { status { isCreated() } }
         .andReturn()

        kafkaTemplate.flush()

        return mockMvcResult

    }

    @Test
    fun shouldSearchByFriendlyId() {
        createDistributionCenter(CreateLocalizablePlaceRestRequest(friendlyId = "search-place", name = "Searchable Place", latitude = 1.0, longitude = 1.0))

        // Wait for Kafka and Elasticsearch to process the events
        await().atMost(Duration.ofSeconds(30)).untilAsserted {

            elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).refresh()

            val result = mockMvc.get("/api/v1/localizable-places/search-by-friendlyid?friendlyId=search") {
                            with(jwt())
                        }.andExpect { status { isOk() } }.andReturn()

            val response = objectMapper.readValue<List<LocalizablePlaceRestResponse>>(result.response.contentAsString)

            val count = response.size
            if (count > 0) {
                assertEquals(1, count)
                assertEquals("Searchable Place", response[0].name)
            }

        }

    }

    @Test
    fun shouldSearchByAdditionalDetail() {
        createDistributionCenter(CreateLocalizablePlaceRestRequest(
            friendlyId = "prop-place",
            name = "Property Place",
            latitude = 2.0,
            longitude = 2.0,
            additionalDetailsMap = mapOf("testKey" to "testValue")
        ))

        // Wait for Kafka and Elasticsearch to process the events
        await().atMost(Duration.ofSeconds(30)).untilAsserted {

            elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).refresh()

            val result = mockMvc.get("/api/v1/localizable-places/search-by-additional-detail?key=testKey&value=testValue") {
                with(jwt())
            }.andExpect { status { isOk() } }.andReturn()

            val response = objectMapper.readValue<List<LocalizablePlaceRestResponse>>(result.response.contentAsString)

            val count = response.size
            if (count > 0) {
                assertEquals(1, count)
                assertEquals("Property Place", response[0].name)
            }

        }

    }

}
