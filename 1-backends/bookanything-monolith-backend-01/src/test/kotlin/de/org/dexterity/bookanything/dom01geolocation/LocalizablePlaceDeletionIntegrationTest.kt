package de.org.dexterity.bookanything.dom01geolocation

import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateLocalizablePlaceRestRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.LocalizablePlaceRestResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Duration
import kotlin.random.Random

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LocalizablePlaceDeletionIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper


    @Test
    fun shouldDeleteALocalizablePlaceById() {
        // Given
        val request = CreateLocalizablePlaceRestRequest(name = "LocalizablePlace To Delete", latitude = -23.55051, longitude = -46.633307)
        val result = createDistributionCenter(request)
        val response = objectMapper.readValue(result.response.contentAsString, LocalizablePlaceRestResponse::class.java)

        // When
        mockMvc.delete("/localizable-places/{id}", response.id) {
            with(jwt())
        }.andExpect { status { isNoContent() } }

        // When
        val resultFromGetById = mockMvc.get("/localizable-places/{id}", response.id) {
            with(jwt())
        }.andExpect { status { isNotFound() } }
            .andReturn()

    }

    @Test
    fun shouldDeleteAllLocalizablePlaces() {

        val newItemsToCreateCount : Int = 10

        // Given
        IntRange(0, (newItemsToCreateCount - 1)).forEach {
            createDistributionCenter(
                CreateLocalizablePlaceRestRequest(
                    name = "One New LocalizablePlace - No.: ${it}",
                    latitude = Random(-23).nextDouble(-23.55999, -23.55000 ),
                    longitude = Random(-46).nextDouble(-46.633999, -46.633000)
                )
            )
        }

        // Wait for Kafka and Elasticsearch to process the events
        await().atMost(Duration.ofSeconds(30)).untilAsserted {

            // When
            val result1 = mockMvc.get("/localizable-places/all") {
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

        // When
        mockMvc.delete("/localizable-places/all") {
            with(jwt())
        }.andExpect { status { isNoContent() } }

        // Wait for Kafka and Elasticsearch to process the events
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            // When
            val result2 = mockMvc.get("/localizable-places/all") {
                with(jwt())
            }.andExpect { status { isOk() } }
                .andReturn()

            // Then
            val responseBody2 = result2.response.contentAsString
            val centrosProximos2 =
                objectMapper.readValue(responseBody2, Array<LocalizablePlaceRestResponse>::class.java).toList()

            assertEquals(0, centrosProximos2.size)
        }
    }

    private fun createDistributionCenter(request: CreateLocalizablePlaceRestRequest): MvcResult {
        return mockMvc.post("/localizable-places") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            with(jwt())
        }.andExpect { status { isCreated() } }
            .andReturn()
    }
}