package de.org.dexterity.bookanything.dom02distributioncenterlocator

import de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.output.persistence.jpa.CentroDistribuicaoJpaRepository
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.events.CentroDistribuicaoCadastradoEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = ["spring.kafka.consumer.group-id=test-group"])
class CentroDistribuicaoIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var centroDistribuicaoJpaRepository: CentroDistribuicaoJpaRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    private val acks: BlockingQueue<CentroDistribuicaoCadastradoEvent> = LinkedBlockingQueue()

    @KafkaListener(topics = ["centro-distribuicao-cadastrado"], groupId = "test-group")
    fun consume(message: CentroDistribuicaoCadastradoEvent) {
        acks.add(message)
    }

    @AfterEach
    fun tearDown() {
        centroDistribuicaoJpaRepository.deleteAll()
    }

    @Test
    fun `should create a new distribution center`() {
        val requestBody = """
            {
                "nome": "CD Teste",
                "latitude": -23.55052,
                "longitude": -46.633308
            }
        """

        val result = mockMvc.post("/cds") {
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
            with(jwt())
        }.andExpect { status { isCreated() } }
            .andReturn()

        val responseBody = result.response.contentAsString
        assertNotNull(responseBody)

        val savedCenters = centroDistribuicaoJpaRepository.findAll()
        assertEquals(1, savedCenters.size)
        assertEquals("CD Teste", savedCenters[0].nome)
    }

    @Test
    fun `should publish CentroDistribuicaoCadastradoEvent to Kafka`() {
        val requestBody = """
            {
                "nome": "CD Kafka Teste",
                "latitude": -20.0,
                "longitude": -40.0
            }
        """

        mockMvc.post("/cds") {
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
            with(jwt())
        }.andExpect { status { isCreated() } }

        val event = acks.poll(10, TimeUnit.SECONDS)
        assertNotNull(event, "Kafka message should have been received")

        assertEquals("CD Kafka Teste", event!!.nome)
        assertEquals(-20.0, event.latitude)
        assertEquals(-40.0, event.longitude)
    }
}