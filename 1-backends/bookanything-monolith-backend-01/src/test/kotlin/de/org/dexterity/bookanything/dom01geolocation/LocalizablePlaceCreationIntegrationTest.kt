package de.org.dexterity.bookanything.dom01geolocation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.org.dexterity.bookanything.dom01geolocation.domain.events.LocalizablePlaceCreatedEvent
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.LocalizablePlaceRestResponse
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.entities.LocalizablePlaceElasticEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.LocalizablePlaceJpaRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.http.MediaType
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = ["spring.kafka.consumer.group-id=test-group"])
class LocalizablePlaceCreationIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var localizablePlaceJpaRepository: LocalizablePlaceJpaRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var elasticsearchOperations: ElasticsearchOperations

    private val acks: BlockingQueue<LocalizablePlaceCreatedEvent> = LinkedBlockingQueue()

    @KafkaListener(topics = ["localizable-place-created-topic"], groupId = "test-group")
    fun consume(message: LocalizablePlaceCreatedEvent) {
        acks.add(message)
    }

    @AfterEach
    fun tearDown() {
        localizablePlaceJpaRepository.deleteAll()
    }

    @Test
    fun shouldCreateANewLocalizablePlace() {
        val requestBody = """
            {
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
    }

    @Test
    fun shouldPublishOneLocalizablePlaceCreateEventToKafka() {
        val requestBody = """
            {
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

        elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).refresh()

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
}