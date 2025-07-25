package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoJsonImported
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.GeoJsonRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoJsonImportJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.GeoJsonImportJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import java.util.*

@Adapter
class GeoJsonJpaAdapter(
    private val geoJsonImportJpaRepository: GeoJsonImportJpaRepository,
    private val geoJsonImportJpaMappers: GeoJsonImportJpaMappers
) : GeoJsonRepositoryPort {

    override fun save(geoJsonImported: GeoJsonImported): GeoJsonImported {
        val entity = geoJsonImportJpaMappers.toEntity(geoJsonImported)
        val savedEntity = geoJsonImportJpaRepository.save(entity)
        return geoJsonImportJpaMappers.toDomain(savedEntity)
    }

    override fun findById(id: UUID): GeoJsonImported? {
        return geoJsonImportJpaRepository.findById(id).map(geoJsonImportJpaMappers::toDomain).orElse(null)
    }

}
