package de.org.dexterity.bookanything.dom02distributioncenterlocator

import de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.input.web.CadastrarCentroDistribuicaoRequest
import de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.output.persistence.elasticsearch.CentroDistribuicaoElasticEntity
import de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.output.persistence.jpa.CentroDistribuicaoJpaRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.http.MediaType
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Duration

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BuscarCentrosProximosIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var centroDistribuicaoJpaRepository: CentroDistribuicaoJpaRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var elasticsearchOperations: ElasticsearchOperations

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry

    @AfterEach
    fun tearDown() {
        centroDistribuicaoJpaRepository.deleteAll()
        elasticsearchOperations.indexOps(CentroDistribuicaoElasticEntity::class.java).delete()
    }

    @Test
    fun `should find distribution centers within a given radius`() {
        // Wait until the consumer for the topic is assigned partitions
        await().atMost(Duration.ofSeconds(10)).until {
            kafkaListenerEndpointRegistry.listenerContainers.any { container ->
                container.groupId == "elasticsearch-indexer" && container.assignedPartitions?.isNotEmpty() == true
            }
        }

        // Given
        createDistributionCenter(CadastrarCentroDistribuicaoRequest(nome = "CD Proximo 0", latitude = -23.55051, longitude = -46.633307))
        createDistributionCenter(CadastrarCentroDistribuicaoRequest(nome = "CD Proximo 1", latitude = -23.55052, longitude = -46.633308))
        createDistributionCenter(CadastrarCentroDistribuicaoRequest(nome = "CD Proximo 2", latitude = -23.55053, longitude = -46.633309))
        createDistributionCenter(CadastrarCentroDistribuicaoRequest(nome = "CD Longe", latitude = -22.9068, longitude = -43.1729))

        // Wait for Kafka and Elasticsearch to process the events
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            val query = CriteriaQuery(Criteria.where("id").exists())
            val searchHits = elasticsearchOperations.search(query, CentroDistribuicaoElasticEntity::class.java)
            val count = searchHits.totalHits
            if (count < 4) {
                val foundIds = searchHits.map { it.content.id }.joinToString()
                println("Documentos encontrados no Elasticsearch: $foundIds")
            }
            assertEquals(4, count)
        }

        val targetLatitude = -23.55052
        val targetLongitude = -46.633308
        val radiusInKm = 1.0 // 1 km

        // When
        val result = mockMvc.get("/cds/search-nearby") {
            param("latitude", targetLatitude.toString())
            param("longitude", targetLongitude.toString())
            param("raioEmKm", radiusInKm.toString())
            with(jwt())
        }.andExpect { status { isOk() } }
            .andReturn()

        // Then
        val responseBody = result.response.contentAsString
        val centrosProximos = objectMapper.readValue(responseBody, Array<de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.input.web.CentroDistribuicaoResponse>::class.java).toList()

        assertEquals(3, centrosProximos.size)
        assert(centrosProximos.any { it.nome == "CD Proximo 0" })
        assert(centrosProximos.any { it.nome == "CD Proximo 1" })
        assert(centrosProximos.any { it.nome == "CD Proximo 2" })
        assert(!centrosProximos.any { it.nome == "CD Longe" })
    }

    private fun createDistributionCenter(request: CadastrarCentroDistribuicaoRequest) {
        mockMvc.post("/cds") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            with(jwt())
        }.andExpect { status { isCreated() } }
        kafkaTemplate.flush()
    }
}