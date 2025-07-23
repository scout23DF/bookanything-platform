package de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.repositories

import de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.entities.BucketEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface BucketJpaRepository : JpaRepository<BucketEntity, Long> {
    fun findByName(name: String): Optional<BucketEntity>
}