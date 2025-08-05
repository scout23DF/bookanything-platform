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
import org.springframework.ai.chat.client.ChatClient
import org.springframework.cloud.openfeign.support.PageJacksonModule
import org.springframework.cloud.openfeign.support.SortJacksonModule
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
        mapper.registerModule(PageJacksonModule())
        mapper.registerModule(SortJacksonModule())
        mapper.registerModule(KotlinModule.Builder().build())
        return mapper
    }

    @Bean
    fun webClient(): WebClient {
        return WebClient.builder()
            .defaultHeader("User-Agent", "BookAnythingBackendApplication/1.0")
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
    fun getSpringAIChatClient(chatClientBuilder: ChatClient.Builder): ChatClient {
        return chatClientBuilder.build()
    }

    @Bean
    fun getSearchEngineInIAProxyPort(chatClient: ChatClient): SearchEngineInIAProxyPort {
        return VertexGeminiIAProxyAdapter(chatClient)
    }

}
