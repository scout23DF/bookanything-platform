package de.org.dexterity.bookanything.dom01geolocation.infrastructure.config

import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationCRUDService
import de.org.dexterity.bookanything.dom01geolocation.application.usecases.*
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers.GeoLocationRestMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BeanConfig {

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
    fun regionUseCase(repository: IRegionRepositoryPort): RegionUseCase = RegionUseCase(repository)

    @Bean
    fun countryUseCase(repository: ICountryRepositoryPort): CountryUseCase = CountryUseCase(repository)

    @Bean
    fun provinceUseCase(repository: IProvinceRepositoryPort): ProvinceUseCase = ProvinceUseCase(repository)

    @Bean
    fun cityUseCase(repository: ICityRepositoryPort): CityUseCase = CityUseCase(repository)

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
        geoLocationRestMapper: GeoLocationRestMapper
    ): GeoLocationCRUDService = GeoLocationCRUDService(
        continentUseCase,
        regionUseCase,
        countryUseCase,
        provinceUseCase,
        cityUseCase,
        districtUseCase,
        geoLocationRestMapper
    )
}

