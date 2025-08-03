package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers

import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.LocalizablePlaceJpaEntity
import de.org.dexterity.bookanything.shared.annotations.Mapper

@Mapper
class LocalizablePlaceJpaMapper() {

    fun toJpaEntity(localizablePlaceModel: LocalizablePlaceModel): LocalizablePlaceJpaEntity {
        return LocalizablePlaceJpaEntity(
            id = localizablePlaceModel.id,
            friendlyId = localizablePlaceModel.friendlyId,
            name = localizablePlaceModel.name,
            alias = localizablePlaceModel.alias,
            additionalDetailsMap = localizablePlaceModel.additionalDetailsMap,
            locationPoint = localizablePlaceModel.locationPoint,
            locationAsGeoHash = localizablePlaceModel.locationAsGeoHash
        )
    }

    fun toDomainModel(localizablePlaceJpaEntity: LocalizablePlaceJpaEntity): LocalizablePlaceModel {
        return LocalizablePlaceModel(
            id = localizablePlaceJpaEntity.id!!,
            friendlyId = localizablePlaceJpaEntity.friendlyId,
            name = localizablePlaceJpaEntity.name!!,
            alias = localizablePlaceJpaEntity.alias,
            additionalDetailsMap = localizablePlaceJpaEntity.additionalDetailsMap,
            locationPoint = localizablePlaceJpaEntity.locationPoint,
            locationAsGeoHash = localizablePlaceJpaEntity.locationAsGeoHash
        )
    }

}
