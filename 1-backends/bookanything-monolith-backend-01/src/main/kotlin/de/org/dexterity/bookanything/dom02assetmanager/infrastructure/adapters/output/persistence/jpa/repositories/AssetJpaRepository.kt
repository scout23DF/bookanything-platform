package de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.repositories

import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetCategory
import de.org.dexterity.bookanything.dom02assetmanager.infrastructure.adapters.output.persistence.jpa.entities.AssetEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AssetJpaRepository : JpaRepository<AssetEntity, Long> {

    fun findByFileNameStartingWith(fileName: String, pageable: Pageable): Page<AssetEntity>

    fun findByBucketName(bucketName: String, pageable: Pageable): Page<AssetEntity>

    fun findByStorageKeyStartingWith(storageKey: String, pageable: Pageable): Page<AssetEntity>

    fun findByCategory(category: AssetCategory, pageable: Pageable): Page<AssetEntity>

    @Query(value= "SELECT a.* FROM tb_asset a WHERE a.json_metadata ->> ?1 = ?2", nativeQuery = true)
    fun findByMetadataContains(@Param("key") key: String, @Param("value") value: String, pageable: Pageable): Page<AssetEntity>

}
