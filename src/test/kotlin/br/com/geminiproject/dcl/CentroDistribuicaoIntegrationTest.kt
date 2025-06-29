package br.com.geminiproject.dcl

import br.com.geminiproject.dcl.adapter.output.persistence.jpa.CentroDistribuicaoJpaRepository
import br.com.geminiproject.dcl.domain.events.CentroDistribuicaoCadastradoEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.Collections
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EmbeddedKafka(partitions = 1, topics = ["centro-distribuicao-cadastrado"])
class CentroDistribuicaoIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var centroDistribuicaoJpaRepository: CentroDistribuicaoJpaRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var embeddedKafka: EmbeddedKafkaBroker

    companion object {
        @Container
        val postgresContainer = PostgreSQLContainer<Nothing>(DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres")).apply {
            withDatabaseName("dcl_test_db")
            withUsername("test")
            withPassword("test")
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
        }
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
        """.trimIndent()

        val result = mockMvc.post("/cds") {
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
            with(jwt())
        }.andExpect { status().isCreated() }
            .andReturn()

        val responseBody = result.response.contentAsString
        assertNotNull(responseBody)

        // Optionally, verify if it's saved in the database
        val savedCenters = centroDistribuicaoJpaRepository.findAll()
        assert(savedCenters.size == 1)
        assert(savedCenters[0].nome == "CD Teste")
    }

    @Test
    fun `should publish CentroDistribuicaoCadastradoEvent to Kafka`() {
        val requestBody = """
            {
                "nome": "CD Kafka Teste",
                "latitude": -20.0,
                "longitude": -40.0
            }
        """.trimIndent()

        val consumerProps = KafkaTestUtils.consumerProps("test-group", "true")
        consumerProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = embeddedKafka.brokersAsString
        val consumer = org.apache.kafka.clients.consumer.KafkaConsumer(consumerProps, StringDeserializer(), StringDeserializer())
        consumer.subscribe(Collections.singletonList("centro-distribuicao-cadastrado"))

        mockMvc.post("/cds") {
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
            with(jwt())
        }.andExpect { status().isCreated() }

        val singleRecord = KafkaTestUtils.getSingleRecord(consumer, "centro-distribuicao-cadastrado")
        val event = objectMapper.readValue(singleRecord.value(), CentroDistribuicaoCadastradoEvent::class.java)

        assertEquals("CD Kafka Teste", event.nome)
        assertEquals(-20.0, event.latitude)
        assertEquals(-40.0, event.longitude)

        consumer.close()
    }
}