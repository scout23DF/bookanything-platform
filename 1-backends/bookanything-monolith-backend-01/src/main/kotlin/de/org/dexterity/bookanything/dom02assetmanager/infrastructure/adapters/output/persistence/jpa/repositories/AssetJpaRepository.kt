package de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.repositories

import de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.entities.AssetEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AssetJpaRepository : JpaRepository<AssetEntity, Long>