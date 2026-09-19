package de.org.dexterity.bookanything.dom01geolocation.domain.ports.enrichment

import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationFlagResult
import de.org.dexterity.bookanything.dom01geolocation.domain.models.IGeoLocationModel

/**
 * Interface defining a provider for official vector flags and vexillological assets.
 */
interface IGeoLocationFlagProvider {
    val providerId: String
    val order: Int
    fun supports(geoLocation: IGeoLocationModel): Boolean = true
    fun getFlag(geoLocation: IGeoLocationModel, parentName: String?): GeoLocationFlagResult?
}
