package br.com.geminiproject.dcl

import br.com.geminiproject.dcl.adapter.input.web.CadastrarCentroDistribuicaoRequest
import br.com.geminiproject.dcl.adapter.output.persistence.elasticsearch.CentroDistribuicaoElasticEntity
import br.com.geminiproject.dcl.adapter.output.persistence.jpa.CentroDistribuicaoJpaRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.http.MediaType
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post
import java.time.Duration
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CentroDistribuicaoExclusaoIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var centroDistribuicaoJpaRepository: CentroDistribuicaoJpaRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var elasticsearchOperations: ElasticsearchOperations

    @Autowired
    private lateinit var kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry

    @AfterEach
    fun tearDown() {
        centroDistribuicaoJpaRepository.deleteAll()
        elasticsearchOperations.indexOps(CentroDistribuicaoElasticEntity::class.java).delete()
    }

    @Test
    fun `should delete a distribution center by id`() {
        // Given
        val request = CadastrarCentroDistribuicaoRequest(nome = "CD Para Deletar", latitude = -23.55051, longitude = -46.633307)
        val result = createDistributionCenter(request)
        val response = objectMapper.readValue(result.response.contentAsString, br.com.geminiproject.dcl.adapter.input.web.CentroDistribuicaoResponse::class.java)

        // When
        mockMvc.delete("/cds/{id}", response.id) {
            with(jwt())
        }.andExpect { status { isNoContent() } }

        // Then
        assertTrue(centroDistribuicaoJpaRepository.findById(response.id).isEmpty)

        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            val query = CriteriaQuery(Criteria.where("id").`is`(response.id.toString()))
            val count = elasticsearchOperations.count(query, CentroDistribuicaoElasticEntity::class.java)
            assertEquals(0, count)
        }
    }

    @Test
    fun `should delete all distribution centers`() {
        // Given
        createDistributionCenter(CadastrarCentroDistribuicaoRequest(nome = "CD 1", latitude = -23.55051, longitude = -46.633307))
        createDistributionCenter(CadastrarCentroDistribuicaoRequest(nome = "CD 2", latitude = -23.55052, longitude = -46.633308))

        // When
        mockMvc.delete("/cds/all") {
            with(jwt())
        }.andExpect { status { isNoContent() } }

        // Then
        assertEquals(0, centroDistribuicaoJpaRepository.count())

        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            val query = CriteriaQuery(Criteria.where("id").exists())
            val count = elasticsearchOperations.count(query, CentroDistribuicaoElasticEntity::class.java)
            assertEquals(0, count)
        }
    }

    private fun createDistributionCenter(request: CadastrarCentroDistribuicaoRequest): org.springframework.test.web.servlet.MvcResult {
        return mockMvc.post("/cds") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            with(jwt())
        }.andExpect { status { isCreated() } }
            .andReturn()
    }
}