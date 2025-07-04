package br.com.geminiproject.dcl

import br.com.geminiproject.dcl.adapter.input.web.CadastrarCentroDistribuicaoRequest
import br.com.geminiproject.dcl.adapter.input.web.CentroDistribuicaoResponse
import br.com.geminiproject.dcl.adapter.output.persistence.elasticsearch.CentroDistribuicaoElasticEntity
import br.com.geminiproject.dcl.adapter.output.persistence.jpa.CentroDistribuicaoJpaRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.data.elasticsearch.core.query.DeleteQuery
import org.springframework.http.MediaType
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Duration
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.get
import java.io.File

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GeoJsonUploadIntegrationTest : AbstractIntegrationTest() {

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

    private val invalidGeoJson = """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "properties": {
                "nome": "CD Invalid"
              },
              "geometry": {
                "type": "Point",
                "coordinates": [-46.633307]
              }
            }
          ]
        }
    """

    @BeforeEach
    fun setup() {
        if (elasticsearchOperations.indexOps(CentroDistribuicaoElasticEntity::class.java).exists()) {
            elasticsearchOperations.indexOps(CentroDistribuicaoElasticEntity::class.java).delete()
        }
        elasticsearchOperations.indexOps(CentroDistribuicaoElasticEntity::class.java).create()
        elasticsearchOperations.indexOps(CentroDistribuicaoElasticEntity::class.java).refresh()
    }

    @AfterEach
    fun tearDown() {
        /*
        centroDistribuicaoJpaRepository.deleteAll()
        elasticsearchOperations.delete(DeleteQuery.builder(CriteriaQuery(Criteria.where("id").exists())).build(), CentroDistribuicaoElasticEntity::class.java)
        await().atMost(Duration.ofSeconds(120)).untilAsserted {
            elasticsearchOperations.indexOps(CentroDistribuicaoElasticEntity::class.java).refresh()
            val count = elasticsearchOperations.count(CriteriaQuery(Criteria.where("id").exists()), CentroDistribuicaoElasticEntity::class.java)
            println("Count in tearDown: $count")
            assertEquals(0, count)
        }
        */
    }

    @Test
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should upload and process GeoJSON file successfully`() {

        val testGeoJSONFile : File = File("src/test/resources/geojson-data/test-electric-vehicle-charging-stations.geojson")
        // Given
        val sampleFile = MockMultipartFile(
            "file",
            "sample.geojson",
            MediaType.APPLICATION_JSON_VALUE,
            testGeoJSONFile.inputStream()
        )
        val newItemsToCreateCount : Int = 22

        // When
        mockMvc.perform(multipart("/cds/upload-geojson")
            .file(sampleFile)
            .param("contentDataType", "geojson")
            .contentType(MediaType.MULTIPART_FORM_DATA)
        ).andExpect(status().isOk())

        // Then
        await().atMost(Duration.ofSeconds(120)).untilAsserted {
            val jpaCount = centroDistribuicaoJpaRepository.count()
            println("JPA count after GeoJSON upload: $jpaCount")
            assertEquals(newItemsToCreateCount, jpaCount)
        }

        await().atMost(Duration.ofSeconds(120)).untilAsserted {
            elasticsearchOperations.indexOps(CentroDistribuicaoElasticEntity::class.java).refresh()
            val esCount = elasticsearchOperations.count(CriteriaQuery(Criteria.where("nome").exists()), CentroDistribuicaoElasticEntity::class.java)
            println("Elasticsearch count after GeoJSON upload: $esCount")
            assertEquals(newItemsToCreateCount, esCount)
        }

    }

    @Test
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should return bad request for invalid GeoJSON file`() {
        // Given
        val invalidFile = MockMultipartFile("file", "invalid.geojson", MediaType.APPLICATION_JSON_VALUE, invalidGeoJson.toByteArray())

        // When
        mockMvc.perform(multipart("/cds/upload-geojson")
            .file(invalidFile)
            .param("contentDataType", "geojson")
            .contentType(MediaType.MULTIPART_FORM_DATA)
        ).andExpect(status().isBadRequest())

        // Then
        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            val jpaCount = centroDistribuicaoJpaRepository.count()
            println("JPA count after invalid GeoJSON upload: $jpaCount")
            assertEquals(0, jpaCount)
        }

        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            elasticsearchOperations.indexOps(CentroDistribuicaoElasticEntity::class.java).refresh()
            val esCount = elasticsearchOperations.count(CriteriaQuery(Criteria.where("nome").exists()), CentroDistribuicaoElasticEntity::class.java)
            println("Elasticsearch count after invalid GeoJSON upload: $esCount")
            assertEquals(0, esCount)
        }
    }

}
