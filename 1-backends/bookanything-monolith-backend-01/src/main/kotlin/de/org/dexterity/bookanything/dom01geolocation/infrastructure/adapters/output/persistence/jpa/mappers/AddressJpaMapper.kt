package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers

import de.org.dexterity.bookanything.dom01geolocation.domain.models.AddressModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoCoordinate
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.AddressEntity
import de.org.dexterity.bookanything.shared.annotations.Mapper
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point

@Mapper
class AddressJpaMapper(private val geoLocationJpaMappers: GeoLocationJpaMappers) {

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
            district = geoLocationJpaMappers.districtToJpaEntity(addressModel.district)
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
            district = geoLocationJpaMappers.districtToDomainModel(addressEntity.district)
        )
    }

    fun buildPointFromGeoCoordinate(sourceGeoCoordinate: GeoCoordinate?): Point? {
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
