package de.org.dexterity.bookanything.dom02assetmanager.domain.ports

import de.org.dexterity.bookanything.dom02assetmanager.domain.models.BucketModel
import java.util.Optional

interface BucketPersistRepositoryPort {
    fun findByName(name: String): Optional<BucketModel>
    fun save(bucket: BucketModel): BucketModel
}
