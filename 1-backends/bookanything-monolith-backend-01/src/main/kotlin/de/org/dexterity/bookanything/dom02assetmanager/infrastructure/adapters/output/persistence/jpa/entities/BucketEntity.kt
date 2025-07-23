package de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.entities

import de.org.dexterity.bookanything.dom02assetmanager.domain.models.StorageProviderType
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "tb_bucket_folder", schema = "public")
class BucketEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val provider: StorageProviderType,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)