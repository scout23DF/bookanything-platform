package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.*
import de.org.dexterity.bookanything.shared.annotations.Mapper
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point

@Mapper
class GeoLocationJpaMapper {

    fun continentToJpaEntity(continentModel: ContinentModel): ContinentEntity {
        return ContinentEntity(
            name = continentModel.name,
            boundaryRepresentation = continentModel.boundaryRepresentation,
            regionsList = emptyList() // continentModel.regionsList?.map { regionToJpaEntity(it) } ?: emptyList()
        )
    }

    fun continentToDomainModel(continentEntity: ContinentEntity): ContinentModel {
        return ContinentModel(
            id = GeoLocationId(continentEntity.id!!),
            name = continentEntity.name,
            boundaryRepresentation = continentEntity.boundaryRepresentation,
            regionsList = emptyList() // continentEntity.regionsList?.map { regionToDomainModel(it) } ?: emptyList()
        )
    }

    fun regionToJpaEntity(regionModel: RegionModel): RegionEntity {
        return RegionEntity(
            name = regionModel.name,
            boundaryRepresentation = regionModel.boundaryRepresentation,
            continent = continentToJpaEntity(regionModel.continent),
            countriesList = emptyList() // regionModel.countriesList?.map { countryToJpaEntity(it) } ?: emptyList()
        )
    }

    fun regionToDomainModel(regionEntity: RegionEntity): RegionModel {
        return RegionModel(
            id = GeoLocationId(regionEntity.id!!),
            name = regionEntity.name,
            boundaryRepresentation = regionEntity.boundaryRepresentation,
            continent = continentToDomainModel(regionEntity.continent),
            countriesList = emptyList() // regionEntity.countriesList?.map { countryToDomainModel(it) } ?: emptyList()
        )
    }

    fun countryToJpaEntity(countryModel: CountryModel): CountryEntity {
        return CountryEntity(
            name = countryModel.name,
            boundaryRepresentation = countryModel.boundaryRepresentation,
            region = regionToJpaEntity(countryModel.region),
            provincesList = emptyList() // countryModel.provincesList?.map { provinceToJpaEntity(it) } ?: emptyList()
        )
    }

    fun countryToDomainModel(countryEntity: CountryEntity): CountryModel {
        return CountryModel(
            id = GeoLocationId(countryEntity.id!!),
            name = countryEntity.name,
            boundaryRepresentation = countryEntity.boundaryRepresentation,
            region = regionToDomainModel(countryEntity.region),
            provincesList = emptyList() // countryEntity.provincesList?.map { provinceToDomainModel(it) } ?: emptyList()
        )
    }

    fun provinceToJpaEntity(provinceModel: ProvinceModel): ProvinceEntity {
        return ProvinceEntity(
            name = provinceModel.name,
            boundaryRepresentation = provinceModel.boundaryRepresentation,
            country = countryToJpaEntity(provinceModel.country),
            citiesList = emptyList() // provinceModel.citiesList?.map { cityToJpaEntity(it) } ?: emptyList()
        )
    }

    fun provinceToDomainModel(provinceEntity: ProvinceEntity): ProvinceModel {
        return ProvinceModel(
            id = GeoLocationId(provinceEntity.id!!),
            name = provinceEntity.name,
            boundaryRepresentation = provinceEntity.boundaryRepresentation,
            country = countryToDomainModel(provinceEntity.country),
            citiesList = emptyList() // provinceEntity.citiesList?.map { cityToDomainModel(it) } ?: emptyList()
        )
    }

    fun cityToJpaEntity(cityModel: CityModel): CityEntity {
        return CityEntity(
            name = cityModel.name,
            boundaryRepresentation = cityModel.boundaryRepresentation,
            province = provinceToJpaEntity(cityModel.province),
            districtsList = emptyList() // cityModel.districtsList?.map { districtToJpaEntity(it) } ?: emptyList()
        )
    }

    fun cityToDomainModel(cityEntity: CityEntity): CityModel {
        return CityModel(
            id = GeoLocationId(cityEntity.id!!),
            name = cityEntity.name,
            boundaryRepresentation = cityEntity.boundaryRepresentation,
            province = provinceToDomainModel(cityEntity.province),
            districtsList = emptyList() // cityEntity.districtsList?.map { districtToDomainModel(it) } ?: emptyList()
        )
    }

    fun districtToJpaEntity(districtModel: DistrictModel): DistrictEntity {
        return DistrictEntity(
            name = districtModel.name,
            boundaryRepresentation = districtModel.boundaryRepresentation,
            city = cityToJpaEntity(districtModel.city),
            addressesList = emptyList() // districtModel.addressesList?.map { addressToJpaEntity(it) } ?: emptyList()
        )
    }

    fun districtToDomainModel(districtEntity: DistrictEntity): DistrictModel {
        return DistrictModel(
            id = GeoLocationId(districtEntity.id!!),
            name = districtEntity.name,
            boundaryRepresentation = districtEntity.boundaryRepresentation,
            city = cityToDomainModel(districtEntity.city),
            addressesList = emptyList() // districtEntity.addressesList?.map { addressToDomainModel(it) } ?: emptyList()
        )
    }

    fun addressToJpaEntity(addressModel: AddressModel): AddressEntity {
        return AddressEntity(
            id = addressModel.id.id,
            streetName = addressModel.streetName,
            houseNumber = addressModel.houseNumber,
            floorNumber = addressModel.floorNumber,
            doorNumber = addressModel.doorNumber,
            addressLine2 = addressModel.addressLine2,
            postalCode = addressModel.postalCode,
            districtName = addressModel.districtName,
            cityName = addressModel.cityName,
            provinceName = addressModel.provinceName,
            countryName = addressModel.countryName,
            coordinates = buildPointFromGeoCoordinate(addressModel.coordinates),
            status = addressModel.status,
            district = districtToJpaEntity(addressModel.district)
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
            district = districtToDomainModel(addressEntity.district)
        )
    }

    private fun buildPointFromGeoCoordinate(sourceGeoCoordinate: GeoCoordinate?): Point? {
        return sourceGeoCoordinate?.let {
            val geometryFactory = GeometryFactory()
            val coordinate = Coordinate(sourceGeoCoordinate.longitude, sourceGeoCoordinate.latitude)
            geometryFactory.createPoint(coordinate)
        }
    }

    private fun buildGeoCoordinateFromPoint(sourcePoint: Point?): GeoCoordinate? {
        return sourcePoint?.let {
            GeoCoordinate(sourcePoint.x, sourcePoint.y)
        }
    }
}