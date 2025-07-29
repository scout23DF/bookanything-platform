package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoJsonFeatureModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoJsonImportedFileModel
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.GeoJsonFeatureEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.GeoJsonImportedFileEntity
import de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.mappers.AssetJpaMapper
import de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.repositories.AssetJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Mapper
import org.springframework.transaction.annotation.Transactional

@Mapper
class GeoJsonImportingJpaMappers(
    private val assetJpaRepository: AssetJpaRepository,
    private val assetJpaMapper: AssetJpaMapper
) {

    @Transactional(readOnly = true)
    fun toGeoJsonImportedFileJpaEntity(sourceDomainModel: GeoJsonImportedFileModel): GeoJsonImportedFileEntity {
        val geoJsonImportedFileEntity = GeoJsonImportedFileEntity(
            id = sourceDomainModel.id,
            fileName = sourceDomainModel.fileName,
            originalContentType = sourceDomainModel.originalContentType,
            importTimestamp = sourceDomainModel.importTimestamp,
            status = sourceDomainModel.status,
            sourceStoredAsset = assetJpaRepository.findById(sourceDomainModel.sourceStoredAsset?.id!!).orElse(null),
            statusDetails = sourceDomainModel.statusDetails
        )
        val featureEntitiesList = sourceDomainModel.featuresList.map { feature ->
            GeoJsonFeatureEntity(
                id = feature.id,
                geoJsonImportedFile = geoJsonImportedFileEntity,
                featureGeometry = feature.featureGeometry,
                featurePropertiesMap = feature.featurePropertiesMap
            )
        }.toMutableList()
        geoJsonImportedFileEntity.featuresList = featureEntitiesList

        return geoJsonImportedFileEntity
    }

    @Transactional(readOnly = true)
    fun toGeoJsonImportedFileModel(sourceJpaEntity: GeoJsonImportedFileEntity): GeoJsonImportedFileModel {

        val tmpAssetEntity = assetJpaRepository.findById(sourceJpaEntity.sourceStoredAsset?.id!!).orElse(null)

        val geoJsonImportedFileModel = GeoJsonImportedFileModel(
            id = sourceJpaEntity.id,
            fileName = sourceJpaEntity.fileName,
            originalContentType = sourceJpaEntity.originalContentType,
            importTimestamp = sourceJpaEntity.importTimestamp,
            status = sourceJpaEntity.status,
            sourceStoredAsset = assetJpaMapper.toDomain(tmpAssetEntity),
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
                featurePropertiesMap = feature.featurePropertiesMap
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
            featurePropertiesMap = sourceDomainModel.featurePropertiesMap
        )
        return geoJsonFeatureJpaEntity

    }

    fun toGeoJsonFeatureModel(sourceJpaEntity: GeoJsonFeatureEntity): GeoJsonFeatureModel {

        val geoJsonFeatureModel = GeoJsonFeatureModel(
            id = sourceJpaEntity.id,
            geoJsonImportedFile = toGeoJsonImportedFileModel(sourceJpaEntity.geoJsonImportedFile),
            featureGeometry = sourceJpaEntity.featureGeometry,
            featurePropertiesMap = sourceJpaEntity.featurePropertiesMap
        )
        return geoJsonFeatureModel

    }

}