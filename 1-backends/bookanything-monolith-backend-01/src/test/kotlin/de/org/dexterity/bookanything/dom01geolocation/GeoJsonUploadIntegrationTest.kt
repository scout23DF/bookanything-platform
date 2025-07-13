package de.org.dexterity.bookanything.dom01geolocation

import com.fasterxml.jackson.databind.ObjectMapper
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.LocalizablePlaceRestResponse
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.entities.LocalizablePlaceElasticEntity
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.File
import java.time.Duration

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GeoJsonUploadIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var elasticsearchOperations: ElasticsearchOperations

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
        if (elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).exists()) {
            elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).delete()
        }
        elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).create()
        elasticsearchOperations.indexOps(LocalizablePlaceElasticEntity::class.java).refresh()
    }

    @Test
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun shouldUploadAndProcessGeoJSONFileSuccessfully() {

        val testGeoJSONFile : File = File("src/test/resources/geojson-data/test-electric-vehicle-charging-stations.geojson")
        // Given
        val sampleFile = MockMultipartFile(
            "file",
            "ev-charging-stations.geojson",
            MediaType.APPLICATION_JSON_VALUE,
            testGeoJSONFile.inputStream()
        )
        val newItemsToCreateCount : Int = 22

        // When
        mockMvc.perform(multipart("/api/v1/localizable-places/upload-geojson")
            .file(sampleFile)
            .param("contentDataType", "ev-charging-stations")
            .contentType(MediaType.MULTIPART_FORM_DATA)
        ).andExpect(status().isOk())

        // Wait for Kafka and Elasticsearch to process the events
        await().atMost(Duration.ofSeconds(30)).untilAsserted {

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

}
