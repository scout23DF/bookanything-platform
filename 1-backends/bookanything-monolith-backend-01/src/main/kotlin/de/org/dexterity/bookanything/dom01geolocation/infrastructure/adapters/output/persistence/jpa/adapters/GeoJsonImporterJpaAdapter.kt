package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoJsonFeatureModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoJsonImportedFileModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.GeoJsonFeatureRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.GeoJsonImportedFileRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoJsonImportingJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.GeoJsonFeatureJpaRepository
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.GeoJsonImportedFileJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import java.util.*

@Adapter
class GeoJsonImporterJpaAdapter(
    private val geoJsonImportedFileJpaRepository: GeoJsonImportedFileJpaRepository,
    private val geoJsonImportingJpaMappers: GeoJsonImportingJpaMappers
) : GeoJsonImportedFileRepositoryPort {

    override fun save(geoJsonImportedFileModel: GeoJsonImportedFileModel): GeoJsonImportedFileModel {
        val entity = geoJsonImportingJpaMappers.toGeoJsonImportedFileJpaEntity(geoJsonImportedFileModel)
        val savedEntity = geoJsonImportedFileJpaRepository.save(entity)
        return geoJsonImportingJpaMappers.toGeoJsonImportedFileModel(savedEntity)
    }

    override fun findById(id: UUID): GeoJsonImportedFileModel? {

        val foundEntity = geoJsonImportedFileJpaRepository.findById(id)

        //.map(geoJsonImportingJpaMappers::toGeoJsonImportedFileModel)
        // .orElse(null)

        return foundEntity.map {

            GeoJsonImportedFileModel(
                id = it.id,
                fileName = it.fileName,
                originalContentType = it.originalContentType,
                importTimestamp = it.importTimestamp,
                status = it.status,
                statusDetails = it.statusDetails
            )

        }.orElse(null)

    }

}

@Adapter
class GeoJsonFeatureAdapter(
    private val geoJsonFeatureJpaRepository: GeoJsonFeatureJpaRepository,
    private val geoJsonImportingJpaMappers: GeoJsonImportingJpaMappers
) : GeoJsonFeatureRepositoryPort {

    override fun save(geoJsonFeatureModel: GeoJsonFeatureModel): GeoJsonFeatureModel {
        val entity = geoJsonImportingJpaMappers.toGeoJsonFeatureJpaEntity(geoJsonFeatureModel)
        val savedEntity = geoJsonFeatureJpaRepository.save(entity)
        return geoJsonImportingJpaMappers.toGeoJsonFeatureModel(savedEntity)
    }

    override fun findById(id: UUID): GeoJsonFeatureModel? {
        return geoJsonFeatureJpaRepository.findById(id).map(geoJsonImportingJpaMappers::toGeoJsonFeatureModel).orElse(null)
    }

    override fun synchronizeFeatureGeometryDataByImportedFileId(geoJsonImportedFileId: UUID) {
        geoJsonFeatureJpaRepository.updateGeometryFromJsonBColumn(geoJsonImportedFileId)
    }

}

