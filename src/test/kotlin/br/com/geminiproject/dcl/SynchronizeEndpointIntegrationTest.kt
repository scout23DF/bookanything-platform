package br.com.geminiproject.dcl

import br.com.geminiproject.dcl.adapter.output.persistence.elasticsearch.CentroDistribuicaoElasticEntity
import br.com.geminiproject.dcl.adapter.output.persistence.jpa.CentroDistribuicaoJpaEntity
import br.com.geminiproject.dcl.adapter.output.persistence.jpa.CentroDistribuicaoJpaRepository
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.data.elasticsearch.core.query.DeleteQuery
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Duration

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SynchronizeEndpointIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var centroDistribuicaoJpaRepository: CentroDistribuicaoJpaRepository

    @Autowired
    private lateinit var elasticsearchOperations: ElasticsearchOperations

    @AfterEach
    fun tearDown() {
        centroDistribuicaoJpaRepository.deleteAll()
        elasticsearchOperations.delete(DeleteQuery.builder(CriteriaQuery(Criteria.where("id").exists())).build(), CentroDistribuicaoElasticEntity::class.java)
    }

    @Test
    fun `should synchronize data from database to elasticsearch`() {
        val geometryFactory = GeometryFactory()
        val centroDistribuicao = CentroDistribuicaoJpaEntity(
            nome = "CD Sync Test",
            localizacao = geometryFactory.createPoint(Coordinate(-20.0, -10.0))
        )
        centroDistribuicaoJpaRepository.save(centroDistribuicao)

        mockMvc.post("/cds/synchronize") {
            with(jwt())
        }.andExpect { status { isOk() } }

        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            val searchHits = elasticsearchOperations.search(
                CriteriaQuery(Criteria.where("nome").`is`("CD Sync Test")),
                CentroDistribuicaoElasticEntity::class.java
            )
            assertEquals(1, searchHits.totalHits)
        }
    }
}