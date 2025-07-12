package de.org.dexterity.bookanything.dom01geolocation

import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.entities.LocalizablePlaceElasticEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.LocalizablePlaceJpaEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.LocalizablePlaceJpaRepository
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
import java.util.UUID
import kotlin.random.Random

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SynchronizeEndpointIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var localizablePlaceJpaRepository: LocalizablePlaceJpaRepository

    @Autowired
    private lateinit var elasticsearchOperations: ElasticsearchOperations

    @AfterEach
    fun tearDown() {
        localizablePlaceJpaRepository.deleteAll()
        elasticsearchOperations.delete(DeleteQuery.builder(CriteriaQuery(Criteria.where("id").exists())).build(), LocalizablePlaceElasticEntity::class.java)
    }

    @Test
    fun shouldSynchronizeDataFromDatabaseToElasticSearch() {
        val geometryFactory = GeometryFactory()

        val newItemsToCreateCount : Int = 100

        IntRange(0, (newItemsToCreateCount - 1)).forEach {
            val centroDistribuicao = LocalizablePlaceJpaEntity(
                id = UUID.randomUUID(),
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


        mockMvc.post("/localizable-places/synchronize") {
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
}