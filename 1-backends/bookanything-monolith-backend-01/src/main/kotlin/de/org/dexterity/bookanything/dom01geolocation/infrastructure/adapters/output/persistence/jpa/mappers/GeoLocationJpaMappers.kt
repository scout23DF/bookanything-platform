package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.*
import de.org.dexterity.bookanything.shared.annotations.Mapper
import org.locationtech.jts.geom.Point

@Mapper
class GeoLocationJpaMappers {

    fun continentToJpaEntity(continentModel: ContinentModel): ContinentEntity {
        return ContinentEntity(
            friendlyId = continentModel.friendlyId,
            name = continentModel.name,
            alias = continentModel.alias,
            propertiesDetailsMap = continentModel.propertiesDetailsMap,
            boundaryRepresentation = continentModel.boundaryRepresentation,
            regionsList = emptyList() // continentModel.regionsList?.map { regionToJpaEntity(it) } ?: emptyList()
        )
    }

    fun continentToDomainModel(continentEntity: ContinentEntity, shouldLoadChildrenList: Boolean): ContinentModel {
        return ContinentModel(
            id = GeoLocationId(continentEntity.id!!),
            friendlyId = continentEntity.friendlyId,
            name = continentEntity.name,
            alias = continentEntity.alias,
            propertiesDetailsMap = continentEntity.propertiesDetailsMap,
            boundaryRepresentation = continentEntity.boundaryRepresentation,
            regionsList = when (shouldLoadChildrenList) {
                true -> continentEntity.regionsList?.map { regionToDomainModel(it, true) } ?: emptyList()
                false -> emptyList()
            }
        )
    }

    fun regionToJpaEntity(regionModel: RegionModel): RegionEntity {
        return RegionEntity(
            friendlyId = regionModel.friendlyId,
            name = regionModel.name,
            alias = regionModel.alias,
            propertiesDetailsMap = regionModel.propertiesDetailsMap,
            boundaryRepresentation = regionModel.boundaryRepresentation,
            continent = continentToJpaEntity(regionModel.continent),
            countriesList = emptyList() // regionModel.countriesList?.map { countryToJpaEntity(it) } ?: emptyList()
        )
    }

    fun regionToDomainModel(regionEntity: RegionEntity, shouldLoadChildrenList: Boolean): RegionModel {
        return RegionModel(
            id = GeoLocationId(regionEntity.id!!),
            friendlyId = regionEntity.friendlyId,
            name = regionEntity.name,
            alias = regionEntity.alias,
            propertiesDetailsMap = regionEntity.propertiesDetailsMap,
            boundaryRepresentation = regionEntity.boundaryRepresentation,
            parentId = regionEntity.continent.id,
            continent = continentToDomainModel(regionEntity.continent, false),
            countriesList = when (shouldLoadChildrenList) {
                true -> regionEntity.countriesList?.map { countryToDomainModel(it, true) } ?: emptyList()
                false -> emptyList()
            }

        )
    }

    fun countryToJpaEntity(countryModel: CountryModel): CountryEntity {
        return CountryEntity(
            friendlyId = countryModel.friendlyId,
            name = countryModel.name,
            alias = countryModel.alias,
            propertiesDetailsMap = countryModel.propertiesDetailsMap,
            boundaryRepresentation = countryModel.boundaryRepresentation,
            region = regionToJpaEntity(countryModel.region),
            provincesList = emptyList() // countryModel.provincesList?.map { provinceToJpaEntity(it) } ?: emptyList()
        )
    }

    fun countryToDomainModel(countryEntity: CountryEntity, shouldLoadChildrenList: Boolean): CountryModel {
        return CountryModel(
            id = GeoLocationId(countryEntity.id!!),
            friendlyId = countryEntity.friendlyId,
            name = countryEntity.name,
            alias = countryEntity.alias,
            propertiesDetailsMap = countryEntity.propertiesDetailsMap,
            boundaryRepresentation = countryEntity.boundaryRepresentation,
            parentId = countryEntity.region.id,
            region = regionToDomainModel(countryEntity.region, false),
            provincesList = when (shouldLoadChildrenList) {
                true -> countryEntity.provincesList?.map { provinceToDomainModel(it, true) } ?: emptyList()
                false -> emptyList()
            }

        )
    }

    fun provinceToJpaEntity(provinceModel: ProvinceModel): ProvinceEntity {
        return ProvinceEntity(
            friendlyId = provinceModel.friendlyId,
            name = provinceModel.name,
            alias = provinceModel.alias,
            propertiesDetailsMap = provinceModel.propertiesDetailsMap,
            boundaryRepresentation = provinceModel.boundaryRepresentation,
            country = countryToJpaEntity(provinceModel.country),
            citiesList = emptyList() // provinceModel.citiesList?.map { cityToJpaEntity(it) } ?: emptyList()
        )
    }

    fun provinceToDomainModel(provinceEntity: ProvinceEntity, shouldLoadChildrenList: Boolean): ProvinceModel {
        return ProvinceModel(
            id = GeoLocationId(provinceEntity.id!!),
            friendlyId = provinceEntity.friendlyId,
            name = provinceEntity.name,
            alias = provinceEntity.alias,
            propertiesDetailsMap = provinceEntity.propertiesDetailsMap,
            boundaryRepresentation = provinceEntity.boundaryRepresentation,
            parentId = provinceEntity.country.id,
            country = countryToDomainModel(provinceEntity.country, false),
            citiesList = when (shouldLoadChildrenList) {
                true -> provinceEntity.citiesList?.map { cityToDomainModel(it, true) } ?: emptyList()
                false -> emptyList()
            }
        )
    }

    fun cityToJpaEntity(cityModel: CityModel): CityEntity {
        return CityEntity(
            friendlyId = cityModel.friendlyId,
            name = cityModel.name,
            alias = cityModel.alias,
            propertiesDetailsMap = cityModel.propertiesDetailsMap,
            boundaryRepresentation = cityModel.boundaryRepresentation,
            province = provinceToJpaEntity(cityModel.province),
            districtsList = emptyList() // cityModel.districtsList?.map { districtToJpaEntity(it) } ?: emptyList()
        )
    }

    fun cityToDomainModel(cityEntity: CityEntity, shouldLoadChildrenList: Boolean): CityModel {
        return CityModel(
            id = GeoLocationId(cityEntity.id!!),
            friendlyId = cityEntity.friendlyId,
            name = cityEntity.name,
            alias = cityEntity.alias,
            propertiesDetailsMap = cityEntity.propertiesDetailsMap,
            boundaryRepresentation = cityEntity.boundaryRepresentation,
            parentId = cityEntity.province.id,
            province = provinceToDomainModel(cityEntity.province, false),
            districtsList = when (shouldLoadChildrenList) {
                true -> cityEntity.districtsList?.map { districtToDomainModel(it, true) } ?: emptyList()
                false -> emptyList()
            }
        )
    }

    fun districtToJpaEntity(districtModel: DistrictModel): DistrictEntity {
        return DistrictEntity(
            friendlyId = districtModel.friendlyId,
            name = districtModel.name,
            alias = districtModel.alias,
            propertiesDetailsMap = districtModel.propertiesDetailsMap,
            boundaryRepresentation = districtModel.boundaryRepresentation,
            city = cityToJpaEntity(districtModel.city),
            addressesList = emptyList() // districtModel.addressesList?.map { addressToJpaEntity(it) } ?: emptyList()
        )
    }

    fun districtToDomainModel(districtEntity: DistrictEntity, shouldLoadChildrenList: Boolean): DistrictModel {
        return DistrictModel(
            id = GeoLocationId(districtEntity.id!!),
            friendlyId = districtEntity.friendlyId,
            name = districtEntity.name,
            alias = districtEntity.alias,
            propertiesDetailsMap = districtEntity.propertiesDetailsMap,
            boundaryRepresentation = districtEntity.boundaryRepresentation,
            parentId = districtEntity.city.id,
            city = cityToDomainModel(districtEntity.city, false),
            addressesList = when (shouldLoadChildrenList) {
                true -> districtEntity.addressesList?.map { addressToDomainModel(it) } ?: emptyList()
                false -> emptyList()
            }

        )
    }

    fun addressToDomainModel(addressEntity: AddressEntity): AddressModel {
        return AddressModel(
            id = GeoLocationId(addressEntity.id!!),
            streetName = addressEntity.streetName,
            houseNumber = addressEntity.houseNumber,
            floorNumber = addressEntity.floorNumber,
            doorNumber = addressEntity.doorNumber,
            addressLine2 = addressEntity.addressLine2,
            postalCode = addressEntity.postalCode,
            districtName = addressEntity.districtName,
            cityName = addressEntity.cityName,
            provinceName = addressEntity.provinceName,
            countryName = addressEntity.countryName,
            coordinates = buildGeoCoordinateFromPoint(addressEntity.coordinates),
            status = addressEntity.status,
            district = districtToDomainModel(addressEntity.district, false)
        )
    }

    private fun buildGeoCoordinateFromPoint(sourcePoint: Point?): GeoCoordinate? {
        return sourcePoint?.let {
            GeoCoordinate(sourcePoint.x, sourcePoint.y)
        }
    }

}