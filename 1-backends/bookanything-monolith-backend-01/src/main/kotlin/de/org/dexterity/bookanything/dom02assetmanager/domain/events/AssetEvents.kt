package de.org.dexterity.bookanything.dom02assetmanager.domain.events

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.HierarchyDetailsRequest

data class AssetRegisteredEvent(
    val assetId: Long,
    val tempFilePath: String,
    val targetCountryCode: String,
    val hierarchyDetailsRequest: HierarchyDetailsRequest

)
