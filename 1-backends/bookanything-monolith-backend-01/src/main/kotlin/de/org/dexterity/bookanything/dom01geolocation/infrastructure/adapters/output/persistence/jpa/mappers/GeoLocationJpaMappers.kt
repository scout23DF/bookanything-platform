package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.*
import de.org.dexterity.bookanything.shared.annotations.Mapper

@Mapper
class GeoLocationJpaMappers {

    fun continentToJpaEntity(continentModel: ContinentModel): ContinentEntity {
        return ContinentEntity(
            name = continentModel.name,
            alias = continentModel.alias,
            boundaryRepresentation = continentModel.boundaryRepresentation,
            regionsList = emptyList() // continentModel.regionsList?.map { regionToJpaEntity(it) } ?: emptyList()
        )
    }

    fun continentToDomainModel(continentEntity: ContinentEntity): ContinentModel {
        return ContinentModel(
            id = GeoLocationId(continentEntity.id!!),
            name = continentEntity.name,
            alias = continentEntity.alias,
            boundaryRepresentation = continentEntity.boundaryRepresentation,
            regionsList = emptyList() // continentEntity.regionsList?.map { regionToDomainModel(it) } ?: emptyList()
        )
    }

    fun regionToJpaEntity(regionModel: RegionModel): RegionEntity {
        return RegionEntity(
            name = regionModel.name,
            alias = regionModel.alias,
            boundaryRepresentation = regionModel.boundaryRepresentation,
            continent = continentToJpaEntity(regionModel.continent),
            countriesList = emptyList() // regionModel.countriesList?.map { countryToJpaEntity(it) } ?: emptyList()
        )
    }

    fun regionToDomainModel(regionEntity: RegionEntity): RegionModel {
        return RegionModel(
            id = GeoLocationId(regionEntity.id!!),
            name = regionEntity.name,
            alias = regionEntity.alias,
            boundaryRepresentation = regionEntity.boundaryRepresentation,
            parentId = regionEntity.continent.id,
            continent = continentToDomainModel(regionEntity.continent),
            countriesList = emptyList() // regionEntity.countriesList?.map { countryToDomainModel(it) } ?: emptyList()
        )
    }

    fun countryToJpaEntity(countryModel: CountryModel): CountryEntity {
        return CountryEntity(
            name = countryModel.name,
            alias = countryModel.alias,
            boundaryRepresentation = countryModel.boundaryRepresentation,
            region = regionToJpaEntity(countryModel.region),
            provincesList = emptyList() // countryModel.provincesList?.map { provinceToJpaEntity(it) } ?: emptyList()
        )
    }

    fun countryToDomainModel(countryEntity: CountryEntity): CountryModel {
        return CountryModel(
            id = GeoLocationId(countryEntity.id!!),
            name = countryEntity.name,
            alias = countryEntity.alias,
            boundaryRepresentation = countryEntity.boundaryRepresentation,
            parentId = countryEntity.region.id,
            region = regionToDomainModel(countryEntity.region),
            provincesList = emptyList() // countryEntity.provincesList?.map { provinceToDomainModel(it) } ?: emptyList()
        )
    }

    fun provinceToJpaEntity(provinceModel: ProvinceModel): ProvinceEntity {
        return ProvinceEntity(
            name = provinceModel.name,
            alias = provinceModel.alias,
            boundaryRepresentation = provinceModel.boundaryRepresentation,
            country = countryToJpaEntity(provinceModel.country),
            citiesList = emptyList() // provinceModel.citiesList?.map { cityToJpaEntity(it) } ?: emptyList()
        )
    }

    fun provinceToDomainModel(provinceEntity: ProvinceEntity): ProvinceModel {
        return ProvinceModel(
            id = GeoLocationId(provinceEntity.id!!),
            name = provinceEntity.name,
            alias = provinceEntity.alias,
            boundaryRepresentation = provinceEntity.boundaryRepresentation,
            parentId = provinceEntity.country.id,
            country = countryToDomainModel(provinceEntity.country),
            citiesList = emptyList() // provinceEntity.citiesList?.map { cityToDomainModel(it) } ?: emptyList()
        )
    }

    fun cityToJpaEntity(cityModel: CityModel): CityEntity {
        return CityEntity(
            name = cityModel.name,
            alias = cityModel.alias,
            boundaryRepresentation = cityModel.boundaryRepresentation,
            province = provinceToJpaEntity(cityModel.province),
            districtsList = emptyList() // cityModel.districtsList?.map { districtToJpaEntity(it) } ?: emptyList()
        )
    }

    fun cityToDomainModel(cityEntity: CityEntity): CityModel {
        return CityModel(
            id = GeoLocationId(cityEntity.id!!),
            name = cityEntity.name,
            alias = cityEntity.alias,
            boundaryRepresentation = cityEntity.boundaryRepresentation,
            parentId = cityEntity.province.id,
            province = provinceToDomainModel(cityEntity.province),
            districtsList = emptyList() // cityEntity.districtsList?.map { districtToDomainModel(it) } ?: emptyList()
        )
    }

    fun districtToJpaEntity(districtModel: DistrictModel): DistrictEntity {
        return DistrictEntity(
            name = districtModel.name,
            alias = districtModel.alias,
            boundaryRepresentation = districtModel.boundaryRepresentation,
            city = cityToJpaEntity(districtModel.city),
            addressesList = emptyList() // districtModel.addressesList?.map { addressToJpaEntity(it) } ?: emptyList()
        )
    }

    fun districtToDomainModel(districtEntity: DistrictEntity): DistrictModel {
        return DistrictModel(
            id = GeoLocationId(districtEntity.id!!),
            name = districtEntity.name,
            alias = districtEntity.alias,
            boundaryRepresentation = districtEntity.boundaryRepresentation,
            parentId = districtEntity.city.id,
            city = cityToDomainModel(districtEntity.city),
            addressesList = emptyList() // districtEntity.addressesList?.map { addressToDomainModel(it) } ?: emptyList()
        )
    }

}