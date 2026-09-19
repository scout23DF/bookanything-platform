package de.org.dexterity.bookanything.dom01geolocation.infrastructure.config

import com.bedatadriven.jackson.datatype.jts.JtsModule
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationCRUDService
import de.org.dexterity.bookanything.dom01geolocation.application.usecases.*
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.ia.gemini.VertexGeminiIAProxyAdapter
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers.GeoLocationRestMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.reactive.function.client.WebClient


@Configuration
class GeoLocationBeansConfig {

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        mapper.registerModule(JavaTimeModule())
        mapper.registerModule(JtsModule())
        mapper.registerModule(KotlinModule.Builder().build())
        return mapper
    }

    @Bean
    fun webClient(): WebClient {
        return WebClient.builder()
            .defaultHeader("User-Agent", "BookAnythingBackendApplication/1.0 (dev@darueira.org)")
            .codecs { configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }
            .build()
    }

    @Bean
    fun localizablePlaceCRUDUseCase(
        localizablePlacePersistRepositoryPort: LocalizablePlacePersistRepositoryPort,
        localizablePlaceQueryRepositoryPort: LocalizablePlaceQueryRepositoryPort,
        eventPublisher: EventPublisherPort
    ): LocalizablePlaceCRUDUseCase {

        return LocalizablePlaceCRUDUseCase(
            localizablePlacePersistRepositoryPort,
            localizablePlaceQueryRepositoryPort,
            eventPublisher
        )
    }

    @Bean
    fun geoJsonFileManagerUseCase(
        localizablePlaceCRUDUseCase: LocalizablePlaceCRUDUseCase,
        geoJsonFilePublisherPort: GeoJsonFilePublisherPort
    ): GeoJsonFileManagerUseCase {

        return GeoJsonFileManagerUseCase(localizablePlaceCRUDUseCase, geoJsonFilePublisherPort)
    }

    @Bean
    fun continentUseCase(repository: IContinentRepositoryPort): ContinentUseCase = ContinentUseCase(repository)

    @Bean
    fun regionUseCase(repository: IRegionRepositoryPort, countryUseCase: CountryUseCase): RegionUseCase = RegionUseCase(repository, countryUseCase)

    @Bean
    fun countryUseCase(repository: ICountryRepositoryPort, provinceUseCase: ProvinceUseCase): CountryUseCase = CountryUseCase(repository, provinceUseCase)

    @Bean
    fun provinceUseCase(repository: IProvinceRepositoryPort, cityUseCase: CityUseCase): ProvinceUseCase = ProvinceUseCase(repository, cityUseCase)

    @Bean
    fun cityUseCase(repository: ICityRepositoryPort, districtUseCase: DistrictUseCase): CityUseCase = CityUseCase(repository, districtUseCase)

    @Bean
    fun districtUseCase(repository: IDistrictRepositoryPort): DistrictUseCase = DistrictUseCase(repository)

    @Bean
    fun addressUseCase(repository: AddressPersistRepositoryPort): AddressUseCase = AddressUseCase(repository)

    @Bean
    fun geoLocationCRUDService(
        continentUseCase: ContinentUseCase,
        regionUseCase: RegionUseCase,
        countryUseCase: CountryUseCase,
        provinceUseCase: ProvinceUseCase,
        cityUseCase: CityUseCase,
        districtUseCase: DistrictUseCase,
        geoLocationRestMapper: GeoLocationRestMapper,
        eventPublisher: EventPublisherPort
    ): GeoLocationCRUDService = GeoLocationCRUDService(
        continentUseCase,
        regionUseCase,
        countryUseCase,
        provinceUseCase,
        cityUseCase,
        districtUseCase,
        geoLocationRestMapper,
        eventPublisher
    )

    @Bean
    fun getSearchEngineInIAProxyPort(
        @org.springframework.beans.factory.annotation.Autowired(required = false)
        chatClientBuilder: ChatClient.Builder? = null,
        @org.springframework.beans.factory.annotation.Autowired(required = false)
        chatModel: org.springframework.ai.chat.model.ChatModel? = null
    ): SearchEngineInIAProxyPort {
        val client = when {
            chatClientBuilder != null -> {
                LoggerFactory.getLogger(GeoLocationBeansConfig::class.java).info("Spring AI: Building ChatClient from ChatClient.Builder...")
                chatClientBuilder.build()
            }
            chatModel != null -> {
                LoggerFactory.getLogger(GeoLocationBeansConfig::class.java).info("Spring AI: Creating ChatClient from ChatModel (${chatModel.javaClass.simpleName})...")
                ChatClient.create(chatModel)
            }
            else -> {
                LoggerFactory.getLogger(GeoLocationBeansConfig::class.java).warn("Spring AI: Neither ChatClient.Builder nor ChatModel available in context!")
                null
            }
        }

        return if (client != null) {
            VertexGeminiIAProxyAdapter(client)
        } else {
            object : SearchEngineInIAProxyPort {
                override fun simpleSearchByPrompt(promptToSearch: String): String? {
                    return "Google Gemini AI is not configured in this environment."
                }
            }
        }
    }

}
