package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.*
import de.org.dexterity.bookanything.shared.annotations.Mapper
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point

/*
@Mapper
class ContinentJpaMapper(val regionJpaMapper: RegionJpaMapper) {

    fun toJpaEntity(continentModel: ContinentModel): ContinentEntity {

        return ContinentEntity(
            name = continentModel.name,
            boundaryRepresentation = continentModel.boundaryRepresentation,
            regionsList = continentModel.regionsList?.map { regionJpaMapper.toJpaEntity(it) } ?: emptyList()
        )
    }

    fun toJpaEntity(continentModel: ContinentModel, persistedEntityToUpdate: ContinentEntity): ContinentEntity {

        return persistedEntityToUpdate.copy(
            name = continentModel.name,
            boundaryRepresentation = continentModel.boundaryRepresentation,
            regionsList = continentModel.regionsList?.map { regionJpaMapper.toJpaEntity(it) } ?: emptyList()
        )
    }

    fun toDomainModel(continentEntity: ContinentEntity): ContinentModel {
        return ContinentModel(
            id = GeoLocationId(continentEntity.id!!),
            name = continentEntity.name,
            boundaryRepresentation = continentEntity.boundaryRepresentation,
            regionsList = continentEntity.regionsList?.map { regionJpaMapper.toDomainModel(it) } ?: emptyList()
        )
    }
}

@Mapper
class RegionJpaMapper(
    val continentJpaMapper: ContinentJpaMapper,
    val countryJpaMapper: CountryJpaMapper
) {

    fun toJpaEntity(regionModel: RegionModel): RegionEntity {
        return RegionEntity(
            name = regionModel.name,
            boundaryRepresentation = regionModel.boundaryRepresentation,
            continent = continentJpaMapper.toJpaEntity(regionModel.continent),
            countriesList = regionModel.countriesList?.map { countryJpaMapper.toJpaEntity(it) } ?: emptyList()
        )
    }

    fun toJpaEntity(regionModel: RegionModel, persistedEntityToUpdate: RegionEntity): RegionEntity {

        return persistedEntityToUpdate.copy(
            name = regionModel.name,
            boundaryRepresentation = regionModel.boundaryRepresentation,
            continent = continentJpaMapper.toJpaEntity(regionModel.continent),
            countriesList = regionModel.countriesList?.map { countryJpaMapper.toJpaEntity(it) } ?: emptyList()
        )
    }

    fun toDomainModel(regionEntity: RegionEntity): RegionModel {
        return RegionModel(
            id = GeoLocationId(regionEntity.id!!),
            name = regionEntity.name,
            boundaryRepresentation = regionEntity.boundaryRepresentation,
            continent = continentJpaMapper.toDomainModel(regionEntity.continent),
            countriesList = regionEntity.countriesList?.map { countryJpaMapper.toDomainModel(it) } ?: emptyList()
        )
    }
}

@Mapper
class CountryJpaMapper(
    val regionJpaMapper: RegionJpaMapper,
    val provinceJpaMapper: ProvinceJpaMapper
) {

    fun toJpaEntity(countryModel: CountryModel): CountryEntity {
        return CountryEntity(
            name = countryModel.name,
            boundaryRepresentation = countryModel.boundaryRepresentation,
            region = regionJpaMapper.toJpaEntity(countryModel.region),
            provincesList = countryModel.provincesList?.map { provinceJpaMapper.toJpaEntity(it) } ?: emptyList()
        )
    }

    fun toJpaEntity(countryModel: CountryModel, persistedEntityToUpdate: CountryEntity): CountryEntity {

        return persistedEntityToUpdate.copy(
            name = countryModel.name,
            boundaryRepresentation = countryModel.boundaryRepresentation,
            region = regionJpaMapper.toJpaEntity(countryModel.region),
            provincesList = countryModel.provincesList?.map { provinceJpaMapper.toJpaEntity(it) } ?: emptyList()
        )
    }

    fun toDomainModel(countryEntity: CountryEntity): CountryModel {
        return CountryModel(
            id = GeoLocationId(countryEntity.id!!),
            name = countryEntity.name,
            boundaryRepresentation = countryEntity.boundaryRepresentation,
            region = regionJpaMapper.toDomainModel(countryEntity.region),
            provincesList = countryEntity.provincesList?.map { provinceJpaMapper.toDomainModel(it) } ?: emptyList()
        )
    }
}

@Mapper
class ProvinceJpaMapper(
    val countryJpaMapper: CountryJpaMapper,
    val cityJpaMapper: CityJpaMapper
) {

    fun toJpaEntity(provinceModel: ProvinceModel): ProvinceEntity {
        return ProvinceEntity(
            name = provinceModel.name,
            boundaryRepresentation = provinceModel.boundaryRepresentation,
            country = countryJpaMapper.toJpaEntity(provinceModel.country),
            citiesList = provinceModel.citiesList?.map { cityJpaMapper.toJpaEntity(it) } ?: emptyList()
        )
    }

    fun toJpaEntity(provinceModel: ProvinceModel, persistedEntityToUpdate: ProvinceEntity): ProvinceEntity {

        return persistedEntityToUpdate.copy(
            name = provinceModel.name,
            boundaryRepresentation = provinceModel.boundaryRepresentation,
            country = countryJpaMapper.toJpaEntity(provinceModel.country),
            citiesList = provinceModel.citiesList?.map { cityJpaMapper.toJpaEntity(it) } ?: emptyList()
        )
    }

    fun toDomainModel(provinceEntity: ProvinceEntity): ProvinceModel {
        return ProvinceModel(
            id = GeoLocationId(provinceEntity.id!!),
            name = provinceEntity.name,
            boundaryRepresentation = provinceEntity.boundaryRepresentation,
            country = countryJpaMapper.toDomainModel(provinceEntity.country),
            citiesList = provinceEntity.citiesList?.map { cityJpaMapper.toDomainModel(it) } ?: emptyList()
        )
    }
}

@Mapper
class CityJpaMapper(
    val provinceJpaMapper: ProvinceJpaMapper,
    val districtJpaMapper: DistrictJpaMapper
) {

    fun toJpaEntity(cityModel: CityModel): CityEntity {
        return CityEntity(
            name = cityModel.name,
            boundaryRepresentation = cityModel.boundaryRepresentation,
            province = provinceJpaMapper.toJpaEntity(cityModel.province),
            districtsList = cityModel.districtsList?.map { districtJpaMapper.toJpaEntity(it) } ?: emptyList()
        )
    }

    fun toJpaEntity(cityModel: CityModel, persistedEntityToUpdate: CityEntity): CityEntity {

        return persistedEntityToUpdate.copy(
            name = cityModel.name,
            boundaryRepresentation = cityModel.boundaryRepresentation,
            province = provinceJpaMapper.toJpaEntity(cityModel.province),
            districtsList = cityModel.districtsList?.map { districtJpaMapper.toJpaEntity(it) } ?: emptyList()
        )
    }

    fun toDomainModel(cityEntity: CityEntity): CityModel {
        return CityModel(
            id = GeoLocationId(cityEntity.id!!),
            name = cityEntity.name,
            boundaryRepresentation = cityEntity.boundaryRepresentation,
            province = provinceJpaMapper.toDomainModel(cityEntity.province),
            districtsList = cityEntity.districtsList?.map { districtJpaMapper.toDomainModel(it) } ?: emptyList()
        )
    }
}

@Mapper
class DistrictJpaMapper(
    val cityJpaMapper: CityJpaMapper,
    val addressJpaMapper: AddressJpaMapper
) {

    fun toJpaEntity(districtModel: DistrictModel): DistrictEntity {
        return DistrictEntity(
            name = districtModel.name,
            boundaryRepresentation = districtModel.boundaryRepresentation,
            city = cityJpaMapper.toJpaEntity(districtModel.city),
            addressesList = districtModel.addressesList?.map { addressJpaMapper.toJpaEntity(it) } ?: emptyList()
        )
    }

    fun toJpaEntity(districtModel: DistrictModel, persistedEntityToUpdate: DistrictEntity): DistrictEntity {

        return persistedEntityToUpdate.copy(
            name = districtModel.name,
            boundaryRepresentation = districtModel.boundaryRepresentation,
            city = cityJpaMapper.toJpaEntity(districtModel.city),
            addressesList = districtModel.addressesList?.map { addressJpaMapper.toJpaEntity(it) } ?: emptyList()
        )
    }

    fun toDomainModel(districtEntity: DistrictEntity): DistrictModel {
        return DistrictModel(
            id = GeoLocationId(districtEntity.id!!),
            name = districtEntity.name,
            boundaryRepresentation = districtEntity.boundaryRepresentation,
            city = cityJpaMapper.toDomainModel(districtEntity.city),
            addressesList = districtEntity.addressesList?.map { addressJpaMapper.toDomainModel(it) } ?: emptyList()
        )
    }
}

@Mapper
class AddressJpaMapper(
    val districtJpaMapper: DistrictJpaMapper
) {

    fun toJpaEntity(addressModel: AddressModel): AddressEntity {
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
            district = districtJpaMapper.toJpaEntity(addressModel.district)
        )
    }

    fun toJpaEntity(addressModel: AddressModel, persistedEntityToUpdate: AddressEntity): AddressEntity {

        return persistedEntityToUpdate.copy(
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
            district = districtJpaMapper.toJpaEntity(addressModel.district)
        )
    }

    fun toDomainModel(addressEntity: AddressEntity): AddressModel {
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
            district = districtJpaMapper.toDomainModel(addressEntity.district)
        )
    }

    private fun buildPointFromGeoCoordinate(sourceGeoCoordinate: GeoCoordinate?): Point? {
        return sourceGeoCoordinate?.let {
            val geometryFactory = GeometryFactory()
            val coordinate = Coordinate(sourceGeoCoordinate.longitude, sourceGeoCoordinate.latitude) // Note: JTS uses (X, Y) which is (longitude, latitude)
            geometryFactory.createPoint(coordinate)
        }
    }

    private fun buildGeoCoordinateFromPoint(sourcePoint: Point?): GeoCoordinate? {
        return sourcePoint?.let {
            GeoCoordinate(sourcePoint.x, sourcePoint.y)
        }
    }

}
*/



@Mapper
class GeoLocationJpaMapper() {

    fun continentToJpaEntity(continentModel: ContinentModel): ContinentEntity {

        return ContinentEntity(
            name = continentModel.name,
            boundaryRepresentation = continentModel.boundaryRepresentation,
            regionsList = continentModel.regionsList?.map { regionToJpaEntity(it) } ?: emptyList()
        )
    }

    fun continentToJpaEntity(continentModel: ContinentModel, persistedEntityToUpdate: ContinentEntity): ContinentEntity {

        return persistedEntityToUpdate.copy(
            name = continentModel.name,
            boundaryRepresentation = continentModel.boundaryRepresentation,
            regionsList = continentModel.regionsList?.map { regionToJpaEntity(it) } ?: emptyList()
        )
    }

    fun continentToDomainModel(continentEntity: ContinentEntity): ContinentModel {
        return ContinentModel(
            id = GeoLocationId(continentEntity.id!!),
            name = continentEntity.name,
            boundaryRepresentation = continentEntity.boundaryRepresentation,
            regionsList = continentEntity.regionsList?.map { regionToDomainModel(it) } ?: emptyList()
        )
    }

    fun regionToJpaEntity(regionModel: RegionModel): RegionEntity {
        return RegionEntity(
            name = regionModel.name,
            boundaryRepresentation = regionModel.boundaryRepresentation,
            continent = continentToJpaEntity(regionModel.continent),
            countriesList = regionModel.countriesList?.map { countryToJpaEntity(it) } ?: emptyList()
        )
    }

    fun regionToJpaEntity(regionModel: RegionModel, persistedEntityToUpdate: RegionEntity): RegionEntity {

        return persistedEntityToUpdate.copy(
            name = regionModel.name,
            boundaryRepresentation = regionModel.boundaryRepresentation,
            continent = continentToJpaEntity(regionModel.continent),
            countriesList = regionModel.countriesList?.map { countryToJpaEntity(it) } ?: emptyList()
        )
    }

    fun regionToDomainModel(regionEntity: RegionEntity): RegionModel {
        return RegionModel(
            id = GeoLocationId(regionEntity.id!!),
            name = regionEntity.name,
            boundaryRepresentation = regionEntity.boundaryRepresentation,
            continent = continentToDomainModel(regionEntity.continent),
            countriesList = regionEntity.countriesList?.map { countryToDomainModel(it) } ?: emptyList()
        )
    }

    fun countryToJpaEntity(countryModel: CountryModel): CountryEntity {
        return CountryEntity(
            name = countryModel.name,
            boundaryRepresentation = countryModel.boundaryRepresentation,
            region = regionToJpaEntity(countryModel.region),
            provincesList = countryModel.provincesList?.map { provinceToJpaEntity(it) } ?: emptyList()
        )
    }

    fun countryToJpaEntity(countryModel: CountryModel, persistedEntityToUpdate: CountryEntity): CountryEntity {

        return persistedEntityToUpdate.copy(
            name = countryModel.name,
            boundaryRepresentation = countryModel.boundaryRepresentation,
            region = regionToJpaEntity(countryModel.region),
            provincesList = countryModel.provincesList?.map { provinceToJpaEntity(it) } ?: emptyList()
        )
    }

    fun countryToDomainModel(countryEntity: CountryEntity): CountryModel {
        return CountryModel(
            id = GeoLocationId(countryEntity.id!!),
            name = countryEntity.name,
            boundaryRepresentation = countryEntity.boundaryRepresentation,
            region = regionToDomainModel(countryEntity.region),
            provincesList = countryEntity.provincesList?.map { provinceToDomainModel(it) } ?: emptyList()
        )
    }

    fun provinceToJpaEntity(provinceModel: ProvinceModel): ProvinceEntity {
        return ProvinceEntity(
            name = provinceModel.name,
            boundaryRepresentation = provinceModel.boundaryRepresentation,
            country = countryToJpaEntity(provinceModel.country),
            citiesList = provinceModel.citiesList?.map { cityToJpaEntity(it) } ?: emptyList()
        )
    }

    fun provinceToJpaEntity(provinceModel: ProvinceModel, persistedEntityToUpdate: ProvinceEntity): ProvinceEntity {

        return persistedEntityToUpdate.copy(
            name = provinceModel.name,
            boundaryRepresentation = provinceModel.boundaryRepresentation,
            country = countryToJpaEntity(provinceModel.country),
            citiesList = provinceModel.citiesList?.map { cityToJpaEntity(it) } ?: emptyList()
        )
    }

    fun provinceToDomainModel(provinceEntity: ProvinceEntity): ProvinceModel {
        return ProvinceModel(
            id = GeoLocationId(provinceEntity.id!!),
            name = provinceEntity.name,
            boundaryRepresentation = provinceEntity.boundaryRepresentation,
            country = countryToDomainModel(provinceEntity.country),
            citiesList = provinceEntity.citiesList?.map { cityToDomainModel(it) } ?: emptyList()
        )
    }

    fun cityToJpaEntity(cityModel: CityModel): CityEntity {
        return CityEntity(
            name = cityModel.name,
            boundaryRepresentation = cityModel.boundaryRepresentation,
            province = provinceToJpaEntity(cityModel.province),
            districtsList = cityModel.districtsList?.map { districtToJpaEntity(it) } ?: emptyList()
        )
    }

    fun cityToJpaEntity(cityModel: CityModel, persistedEntityToUpdate: CityEntity): CityEntity {

        return persistedEntityToUpdate.copy(
            name = cityModel.name,
            boundaryRepresentation = cityModel.boundaryRepresentation,
            province = provinceToJpaEntity(cityModel.province),
            districtsList = cityModel.districtsList?.map { districtToJpaEntity(it) } ?: emptyList()
        )
    }

    fun cityToDomainModel(cityEntity: CityEntity): CityModel {
        return CityModel(
            id = GeoLocationId(cityEntity.id!!),
            name = cityEntity.name,
            boundaryRepresentation = cityEntity.boundaryRepresentation,
            province = provinceToDomainModel(cityEntity.province),
            districtsList = cityEntity.districtsList?.map { districtToDomainModel(it) } ?: emptyList()
        )
    }

    fun districtToJpaEntity(districtModel: DistrictModel): DistrictEntity {
        return DistrictEntity(
            name = districtModel.name,
            boundaryRepresentation = districtModel.boundaryRepresentation,
            city = cityToJpaEntity(districtModel.city),
            addressesList = districtModel.addressesList?.map { addressToJpaEntity(it) } ?: emptyList()
        )
    }

    fun districtToJpaEntity(districtModel: DistrictModel, persistedEntityToUpdate: DistrictEntity): DistrictEntity {

        return persistedEntityToUpdate.copy(
            name = districtModel.name,
            boundaryRepresentation = districtModel.boundaryRepresentation,
            city = cityToJpaEntity(districtModel.city),
            addressesList = districtModel.addressesList?.map { addressToJpaEntity(it) } ?: emptyList()
        )
    }

    fun districtToDomainModel(districtEntity: DistrictEntity): DistrictModel {
        return DistrictModel(
            id = GeoLocationId(districtEntity.id!!),
            name = districtEntity.name,
            boundaryRepresentation = districtEntity.boundaryRepresentation,
            city = cityToDomainModel(districtEntity.city),
            addressesList = districtEntity.addressesList?.map { addressToDomainModel(it) } ?: emptyList()
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

    fun addressToJpaEntity(addressModel: AddressModel, persistedEntityToUpdate: AddressEntity): AddressEntity {

        return persistedEntityToUpdate.copy(
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
            val coordinate = Coordinate(sourceGeoCoordinate.longitude, sourceGeoCoordinate.latitude) // Note: JTS uses (X, Y) which is (longitude, latitude)
            geometryFactory.createPoint(coordinate)
        }
    }

    private fun buildGeoCoordinateFromPoint(sourcePoint: Point?): GeoCoordinate? {
        return sourcePoint?.let {
            GeoCoordinate(sourcePoint.x, sourcePoint.y)
        }
    }

}
