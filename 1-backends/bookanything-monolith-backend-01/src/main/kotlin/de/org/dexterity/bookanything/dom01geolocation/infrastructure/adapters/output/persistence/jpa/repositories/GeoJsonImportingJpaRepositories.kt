package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories

import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.GeoJsonFeatureEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.GeoJsonImportedFileEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
interface GeoJsonImportedFileJpaRepository : JpaRepository<GeoJsonImportedFileEntity, UUID> {

    fun existsByFileName(fileName: String): Boolean

}

@Repository
interface GeoJsonFeatureJpaRepository : JpaRepository<GeoJsonFeatureEntity, UUID> {

    @Modifying()
    @Transactional
    @Query(
        "UPDATE tb_geojson_feature SET ge_feature_geometry = ST_GeomFromGeoJSON(json_geometry_content) WHERE geojson_imported_file_id = :paramGeoJsonImportedFileId",
        nativeQuery = true
    )
    fun updateGeometryFromJsonBColumn(@Param("paramGeoJsonImportedFileId") paramGeoJsonImportedFileId: UUID)

}

