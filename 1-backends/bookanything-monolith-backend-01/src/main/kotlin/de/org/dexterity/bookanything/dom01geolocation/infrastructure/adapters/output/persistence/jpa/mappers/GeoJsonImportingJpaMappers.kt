package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoJsonFeatureModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoJsonImportedFileModel
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.GeoJsonFeatureEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.GeoJsonImportedFileEntity
import de.org.dexterity.bookanything.shared.annotations.Mapper

@Mapper
class GeoJsonImportingJpaMappers() {

    fun toGeoJsonImportedFileJpaEntity(sourceDomainModel: GeoJsonImportedFileModel): GeoJsonImportedFileEntity {
        val geoJsonImportedFileEntity = GeoJsonImportedFileEntity(
            id = sourceDomainModel.id,
            fileName = sourceDomainModel.fileName,
            originalContentType = sourceDomainModel.originalContentType,
            importTimestamp = sourceDomainModel.importTimestamp,
            status = sourceDomainModel.status,
            statusDetails = sourceDomainModel.statusDetails
        )
        val featureEntitiesList = sourceDomainModel.featuresList.map { feature ->
            GeoJsonFeatureEntity(
                id = feature.id,
                geoJsonImportedFile = geoJsonImportedFileEntity,
                featureGeometry = feature.featureGeometry,
                featurePropertiesMap = feature.featurePropertiesMap,
                featureGeometryContentAsJson = feature.featureGeometryContentAsJson
            )
        }.toMutableList()
        geoJsonImportedFileEntity.featuresList = featureEntitiesList

        return geoJsonImportedFileEntity
    }

    fun toGeoJsonImportedFileModel(sourceJpaEntity: GeoJsonImportedFileEntity): GeoJsonImportedFileModel {

        val geoJsonImportedFileModel = GeoJsonImportedFileModel(
            id = sourceJpaEntity.id,
            fileName = sourceJpaEntity.fileName,
            originalContentType = sourceJpaEntity.originalContentType,
            importTimestamp = sourceJpaEntity.importTimestamp,
            status = sourceJpaEntity.status,
            statusDetails = sourceJpaEntity.statusDetails
        )

        val featureModelsList = sourceJpaEntity.featuresList.map { feature ->
            GeoJsonFeatureModel(
                id = feature.id,
                geoJsonImportedFile = GeoJsonImportedFileModel(
                    id = geoJsonImportedFileModel.id,
                    fileName = geoJsonImportedFileModel.fileName,
                    originalContentType = geoJsonImportedFileModel.originalContentType,
                    importTimestamp = geoJsonImportedFileModel.importTimestamp,
                    status = geoJsonImportedFileModel.status,
                    statusDetails = geoJsonImportedFileModel.statusDetails
                ),
                featureGeometry = feature.featureGeometry,
                featurePropertiesMap = feature.featurePropertiesMap,
                featureGeometryContentAsJson = feature.featureGeometryContentAsJson
            )
        }.toMutableList()
        geoJsonImportedFileModel.featuresList = featureModelsList

        return geoJsonImportedFileModel
    }

    fun toGeoJsonFeatureJpaEntity(sourceDomainModel: GeoJsonFeatureModel): GeoJsonFeatureEntity {

        val geoJsonFeatureJpaEntity = GeoJsonFeatureEntity(
            id = sourceDomainModel.id,
            geoJsonImportedFile = toGeoJsonImportedFileJpaEntity(sourceDomainModel.geoJsonImportedFile),
            featureGeometry = sourceDomainModel.featureGeometry,
            featurePropertiesMap = sourceDomainModel.featurePropertiesMap,
            featureGeometryContentAsJson = sourceDomainModel.featureGeometryContentAsJson
        )
        return geoJsonFeatureJpaEntity

    }

    fun toGeoJsonFeatureModel(sourceJpaEntity: GeoJsonFeatureEntity): GeoJsonFeatureModel {

        val geoJsonFeatureModel = GeoJsonFeatureModel(
            id = sourceJpaEntity.id,
            geoJsonImportedFile = toGeoJsonImportedFileModel(sourceJpaEntity.geoJsonImportedFile),
            featureGeometry = sourceJpaEntity.featureGeometry,
            featurePropertiesMap = sourceJpaEntity.featurePropertiesMap,
            featureGeometryContentAsJson = sourceJpaEntity.featureGeometryContentAsJson
        )
        return geoJsonFeatureModel

    }

}