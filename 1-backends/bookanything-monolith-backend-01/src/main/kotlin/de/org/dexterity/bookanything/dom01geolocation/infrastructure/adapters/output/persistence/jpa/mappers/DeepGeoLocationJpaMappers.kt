package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.*
import de.org.dexterity.bookanything.shared.annotations.Mapper
import org.locationtech.jts.geom.Point

@Mapper
class DeepGeoLocationJpaMappers() {

    fun deepContinentToDomainModel(continentEntity: ContinentEntity, shouldLoadChildrenList: Boolean): ContinentModel {
        return ContinentModel(
            id = GeoLocationId(continentEntity.id!!),
            friendlyId = continentEntity.friendlyId,
            name = continentEntity.name,
            alias = continentEntity.alias,
            propertiesDetailsMap = continentEntity.propertiesDetailsMap,
            boundaryRepresentation = continentEntity.boundaryRepresentation,
            regionsList = when (shouldLoadChildrenList) {
                true -> continentEntity.regionsList?.map { deepRegionToDomainModel(it, true) } ?: emptyList()
                false -> emptyList()
            }
        )
    }

    fun deepRegionToDomainModel(regionEntity: RegionEntity, shouldLoadChildrenList: Boolean): RegionModel {
        return RegionModel(
            id = GeoLocationId(regionEntity.id!!),
            friendlyId = regionEntity.friendlyId,
            name = regionEntity.name,
            alias = regionEntity.alias,
            propertiesDetailsMap = regionEntity.propertiesDetailsMap,
            boundaryRepresentation = regionEntity.boundaryRepresentation,
            parentId = regionEntity.continent.id,
            continent = deepContinentToDomainModel(regionEntity.continent, false),
            countriesList = when (shouldLoadChildrenList) {
                true -> regionEntity.countriesList?.map { deepCountryToDomainModel(it, true) } ?: emptyList()
                false -> emptyList()
            }
        )
    }

    fun deepCountryToDomainModel(countryEntity: CountryEntity, shouldLoadChildrenList: Boolean): CountryModel {
        return CountryModel(
            id = GeoLocationId(countryEntity.id!!),
            friendlyId = countryEntity.friendlyId,
            name = countryEntity.name,
            alias = countryEntity.alias,
            propertiesDetailsMap = countryEntity.propertiesDetailsMap,
            boundaryRepresentation = countryEntity.boundaryRepresentation,
            parentId = countryEntity.region.id,
            region = deepRegionToDomainModel(countryEntity.region, false),
            provincesList = when (shouldLoadChildrenList) {
                true -> countryEntity.provincesList?.map { deepProvinceToDomainModel(it, true) } ?: emptyList()
                false -> emptyList()
            }
        )
    }

    fun deepProvinceToDomainModel(provinceEntity: ProvinceEntity, shouldLoadChildrenList: Boolean): ProvinceModel {
        return ProvinceModel(
            id = GeoLocationId(provinceEntity.id!!),
            friendlyId = provinceEntity.friendlyId,
            name = provinceEntity.name,
            alias = provinceEntity.alias,
            propertiesDetailsMap = provinceEntity.propertiesDetailsMap,
            boundaryRepresentation = provinceEntity.boundaryRepresentation,
            parentId = provinceEntity.country.id,
            country = deepCountryToDomainModel(provinceEntity.country, false),
            citiesList = when (shouldLoadChildrenList) {
                true -> provinceEntity.citiesList?.map { deepCityToDomainModel(it, true) } ?: emptyList()
                false -> emptyList()
            }
        )
    }

    fun deepCityToDomainModel(cityEntity: CityEntity, shouldLoadChildrenList: Boolean): CityModel {
        return CityModel(
            id = GeoLocationId(cityEntity.id!!),
            friendlyId = cityEntity.friendlyId,
            name = cityEntity.name,
            alias = cityEntity.alias,
            propertiesDetailsMap = cityEntity.propertiesDetailsMap,
            boundaryRepresentation = cityEntity.boundaryRepresentation,
            parentId = cityEntity.province.id,
            province = deepProvinceToDomainModel(cityEntity.province, false),
            districtsList = when (shouldLoadChildrenList) {
                true -> cityEntity.districtsList?.map { deepDistrictToDomainModel(it, true) } ?: emptyList()
                false -> emptyList()
            }
        )
    }

    fun deepDistrictToDomainModel(districtEntity: DistrictEntity, shouldLoadChildrenList: Boolean): DistrictModel {
        return DistrictModel(
            id = GeoLocationId(districtEntity.id!!),
            friendlyId = districtEntity.friendlyId,
            name = districtEntity.name,
            alias = districtEntity.alias,
            propertiesDetailsMap = districtEntity.propertiesDetailsMap,
            boundaryRepresentation = districtEntity.boundaryRepresentation,
            parentId = districtEntity.city.id,
            city = deepCityToDomainModel(districtEntity.city, false),
            addressesList = when (shouldLoadChildrenList) {
                true -> districtEntity.addressesList?.map { deepAddressToDomainModel(it) } ?: emptyList()
                false -> emptyList()
            }
        )
    }

    fun deepAddressToDomainModel(addressEntity: AddressEntity): AddressModel {
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
            district = deepDistrictToDomainModel(addressEntity.district, false)
        )
    }

    private fun buildGeoCoordinateFromPoint(sourcePoint: Point?): GeoCoordinate? {
        return sourcePoint?.let {
            GeoCoordinate(sourcePoint.x, sourcePoint.y)
        }
    }

}