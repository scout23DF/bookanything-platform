package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoJsonImportStatus
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.locationtech.jts.geom.Geometry
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "tb_geojson_imported_file")
data class GeoJsonImportedEntity(
    @Id
    val id: UUID,

    @Column(name = "ds_filename", nullable = false)
    val fileName: String,

    @Column(name = "ds_original_content_type", nullable = false)
    val originalContentType: String,

    @Column(name = "ts_import", nullable = false)
    val importTimestamp: Instant,

    @Enumerated(EnumType.STRING)
    @Column(name = "cd_import_status", nullable = false)
    var status: GeoJsonImportStatus,

    @Column(name = "ds_status_details")
    var statusDetails: String? = null,

    @OneToMany(mappedBy = "geoJsonImportedFile", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var featuresList: MutableList<GeoJsonFeatureEntity> = mutableListOf()
)

@Entity
@Table(name = "tb_geojson_feature")
data class GeoJsonFeatureEntity(
    @Id
    val id: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geojson_imported_file_id", nullable = false)
    val geoJsonImportedFile: GeoJsonImportedEntity,

    @Column(name = "ge_feature_geometry", columnDefinition = "geometry", nullable = false)
    val featureGeometry: Geometry,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_feature_properties", columnDefinition = "jsonb")
    val featurePropertiesMap: Map<String, Any?>
)
