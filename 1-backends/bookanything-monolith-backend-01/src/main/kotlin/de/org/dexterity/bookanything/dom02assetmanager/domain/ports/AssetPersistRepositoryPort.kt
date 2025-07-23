package de.org.dexterity.bookanything.dom02assetmanager.domain.ports

import de.org.dexterity.bookanything.dom02assetmanager.domain.models.AssetModel
import java.util.Optional

interface AssetPersistRepositoryPort {
    fun save(asset: AssetModel): AssetModel
    fun findById(id: Long): Optional<AssetModel>
}
