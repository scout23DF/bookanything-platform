package de.org.dexterity.bookanything.dom01geolocation.domain.ports

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoJsonFeatureModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoJsonImportedFileModel
import java.util.*

interface GeoJsonImportedFileRepositoryPort {
    fun save(geoJsonImportedFileModel: GeoJsonImportedFileModel): GeoJsonImportedFileModel
    fun findById(id: UUID): GeoJsonImportedFileModel?
}

interface GeoJsonFeatureRepositoryPort {
    fun save(geoJsonFeatureModel: GeoJsonFeatureModel): GeoJsonFeatureModel
    fun findById(id: UUID): GeoJsonFeatureModel?
    fun synchronizeFeatureGeometryDataByImportedFileId(geoJsonImportedFileId: UUID) {}
}
