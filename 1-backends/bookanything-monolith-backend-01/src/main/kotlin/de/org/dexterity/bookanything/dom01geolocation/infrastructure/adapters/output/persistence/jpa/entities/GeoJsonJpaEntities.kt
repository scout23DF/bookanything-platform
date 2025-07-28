package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities

import org.geolatte.geom.Geometry
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoJsonImportStatus
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

@Entity
@Table(name = "tb_geojson_imported_file")
data class GeoJsonImportedFileEntity(
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

    @OneToMany(mappedBy = "geoJsonImportedFile", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    var featuresList: MutableList<GeoJsonFeatureEntity> = mutableListOf()
)

@Entity
@Table(name = "tb_geojson_feature")
data class GeoJsonFeatureEntity(
    @Id
    val id: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geojson_imported_file_id", nullable = false)
    val geoJsonImportedFile: GeoJsonImportedFileEntity,

    @Column(name = "ge_feature_geometry", columnDefinition = "geometry(MultiPolygon,4326)", nullable = true)
    @JdbcTypeCode(SqlTypes.GEOMETRY)
    val featureGeometry: Geometry<*>? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_feature_properties", columnDefinition = "jsonb")
    val featurePropertiesMap: Map<String, Any?>,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_geometry_content", columnDefinition = "jsonb")
    val featureGeometryContentAsJson: String? = null

)
