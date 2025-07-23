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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bucket_id", nullable = false)
    val bucket: BucketEntity,

    @Column(nullable = false)
    val fileName: String,

    @Column(nullable = false, unique = true)
    val storageKey: String,

    @Column(nullable = false)
    val mimeType: String,

    @Column(nullable = false)
    val size: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val category: AssetCategory,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val metadata: Map<String, Any> = mutableMapOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: AssetStatus,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}