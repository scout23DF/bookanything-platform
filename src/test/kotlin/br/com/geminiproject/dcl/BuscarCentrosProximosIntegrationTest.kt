package br.com.geminiproject.dcl

import br.com.geminiproject.dcl.adapter.output.persistence.jpa.CentroDistribuicaoJpaEntity
import br.com.geminiproject.dcl.adapter.output.persistence.jpa.CentroDistribuicaoJpaRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BuscarCentrosProximosIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var centroDistribuicaoJpaRepository: CentroDistribuicaoJpaRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    companion object {
        @Container
        val postgresContainer = PostgreSQLContainer<Nothing>(DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres")).apply {
            withDatabaseName("dcl_test_db")
            withUsername("test")
            withPassword("test")
        }

        @JvmStatic
        @org.springframework.test.context.DynamicPropertySource
        fun properties(registry: org.springframework.test.context.DynamicPropertyRegistry) {
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
    fun `should find distribution centers within a given radius`() {
        // Given
        val cd1 = CentroDistribuicaoJpaEntity(
            id = UUID.randomUUID(),
            nome = "CD Proximo 1",
            localizacao = geometryFactory.createPoint(Coordinate(-46.633308, -23.55052)) // São Paulo
        )
        val cd2 = CentroDistribuicaoJpaEntity(
            id = UUID.randomUUID(),
            nome = "CD Proximo 2",
            localizacao = geometryFactory.createPoint(Coordinate(-46.633308, -23.55052)) // Same as São Paulo
        )
        val cd3 = CentroDistribuicaoJpaEntity(
            id = UUID.randomUUID(),
            nome = "CD Longe",
            localizacao = geometryFactory.createPoint(Coordinate(-43.1729, -22.9068)) // Rio de Janeiro
        )
        centroDistribuicaoJpaRepository.saveAll(listOf(cd1, cd2, cd3))

        val targetLatitude = -23.55052
        val targetLongitude = -46.633308
        val radiusInKm = 1.0 // 1 km

        // When
        val result = mockMvc.get("/cds/search-nearby") {
            param("latitude", targetLatitude.toString())
            param("longitude", targetLongitude.toString())
            param("raioEmKm", radiusInKm.toString())
            with(jwt())
        }.andExpect { status().isOk() }
            .andReturn()

        // Then
        val responseBody = result.response.contentAsString
        val centrosProximos = objectMapper.readValue(responseBody, Array<br.com.geminiproject.dcl.adapter.input.web.CentroDistribuicaoResponse>::class.java).toList()

        assertEquals(2, centrosProximos.size)
        assert(centrosProximos.any { it.nome == "CD Proximo 1" })
        assert(centrosProximos.any { it.nome == "CD Proximo 2" })
        assert(!centrosProximos.any { it.nome == "CD Longe" })
    }
}
