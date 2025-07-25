package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoJsonFeature
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoJsonImported
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.GeoJsonFeatureEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.GeoJsonImportedEntity
import de.org.dexterity.bookanything.shared.annotations.Mapper

@Mapper
class GeoJsonImportJpaMappers() {

    fun toEntity(domain: GeoJsonImported): GeoJsonImportedEntity {
        val importEntity = GeoJsonImportedEntity(
            id = domain.id,
            fileName = domain.fileName,
            originalContentType = domain.originalContentType,
            importTimestamp = domain.importTimestamp,
            status = domain.status,
            statusDetails = domain.statusDetails
        )
        val featureEntities = domain.featuresList.map { feature ->
            GeoJsonFeatureEntity(
                id = feature.id,
                geoJsonImportedFile = importEntity,
                featureGeometry = feature.featureGeometry,
                featurePropertiesMap = feature.featurePropertiesMap
            )
        }.toMutableList()
        importEntity.featuresList = featureEntities
        return importEntity
    }

    fun toDomain(entity: GeoJsonImportedEntity): GeoJsonImported {
        val domain = GeoJsonImported(
            id = entity.id,
            fileName = entity.fileName,
            originalContentType = entity.originalContentType,
            importTimestamp = entity.importTimestamp,
            status = entity.status,
            statusDetails = entity.statusDetails
        )
        val features = entity.featuresList.map { featureEntity ->
            GeoJsonFeature(
                id = featureEntity.id,
                geoJsonImportedFileId = domain.id,
                featureGeometry = featureEntity.featureGeometry,
                featurePropertiesMap = featureEntity.featurePropertiesMap
            )
        }.toList()
        domain.featuresList = features
        return domain
    }

}