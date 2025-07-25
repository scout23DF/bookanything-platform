package de.org.dexterity.bookanything.dom01geolocation.domain.ports

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoJsonImported
import java.util.UUID

interface GeoJsonRepositoryPort {
    fun save(geoJsonImported: GeoJsonImported): GeoJsonImported
    fun findById(id: UUID): GeoJsonImported?
}
