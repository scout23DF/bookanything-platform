package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.mappers

import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.entities.LocalizablePlaceElasticEntity
import de.org.dexterity.bookanything.shared.annotations.Mapper
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.springframework.data.elasticsearch.core.geo.GeoPoint

@Mapper
class LocalizablePlaceElasticMapper() {

    fun toElasticEntity(localizablePlaceModel: LocalizablePlaceModel): LocalizablePlaceElasticEntity {
        return LocalizablePlaceElasticEntity(
            id = localizablePlaceModel.id,
            friendlyId = localizablePlaceModel.friendlyId,
            name = localizablePlaceModel.name,
            alias = localizablePlaceModel.alias,
            additionalDetailsMap = localizablePlaceModel.additionalDetailsMap,
            locationPoint = GeoPoint(
                localizablePlaceModel.locationPoint?.y ?: 0.0,
                localizablePlaceModel.locationPoint?.x ?: 0.0
            ),
            locationAsGeoHash = localizablePlaceModel.locationAsGeoHash
        )
    }

    fun toDomainModel(localizablePlaceElasticEntity: LocalizablePlaceElasticEntity): LocalizablePlaceModel {
        return LocalizablePlaceModel(
            id = localizablePlaceElasticEntity.id,
            friendlyId = localizablePlaceElasticEntity.friendlyId,
            name = localizablePlaceElasticEntity.name,
            alias = localizablePlaceElasticEntity.alias,
            additionalDetailsMap = localizablePlaceElasticEntity.additionalDetailsMap,
            locationPoint = GeometryFactory().createPoint(
                Coordinate(
                    localizablePlaceElasticEntity.locationPoint.lon,
                    localizablePlaceElasticEntity.locationPoint.lat
                )
            ),
            locationAsGeoHash = localizablePlaceElasticEntity.locationAsGeoHash
        )
    }

}
