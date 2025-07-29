package de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.entities

import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetCategory
import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetStatus
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "tb_asset", schema = "public")
class AssetEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "ds_filename", nullable = false)
    val fileName: String,

    @Column(name="ds_storage_key", nullable = false, unique = true)
    val storageKey: String,

    @Column(name="ds_mime_type", nullable = false)
    val mimeType: String,

    @Column(name="vr_file_size", nullable = false)
    val size: Long,

    @Enumerated(EnumType.STRING)
    @Column(name="tp_category", nullable = false)
    val category: AssetCategory,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="json_metadata", columnDefinition = "jsonb")
    val metadata: Map<String, Any> = mutableMapOf(),

    @Enumerated(EnumType.STRING)
    @Column(name="cd_status", nullable = false)
    var status: AssetStatus,

    @Column(name="ts_created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name="ts_updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bucket_id", nullable = false)
    val bucket: BucketEntity

) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}