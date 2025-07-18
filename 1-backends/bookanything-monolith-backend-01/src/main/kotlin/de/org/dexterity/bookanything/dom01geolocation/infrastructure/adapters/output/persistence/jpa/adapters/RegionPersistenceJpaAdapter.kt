package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.models.RegionModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.IRegionRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.RegionEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.DeepGeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.ContinentJpaRepository
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.RegionJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import java.util.*

@Adapter
class RegionPersistenceJpaAdapter(
    val regionJpaRepository: RegionJpaRepository,
    val continentJpaRepository: ContinentJpaRepository,
    val geoLocationJpaMappers: GeoLocationJpaMappers,
    val deepGeoLocationJpaMappers: DeepGeoLocationJpaMappers
) : IRegionRepositoryPort {

    override fun saveNew(targetModel: RegionModel): RegionModel {
        val continentEntity = continentJpaRepository.findById(targetModel.continent.id.id).orElseThrow()
        val regionEntity = RegionEntity(
            name = targetModel.name,
            boundaryRepresentation = targetModel.boundaryRepresentation,
            continent = continentEntity
        )
        val entitySaved = regionJpaRepository.save(regionEntity)
        return geoLocationJpaMappers.regionToDomainModel(entitySaved)
    }

    override fun update(targetModel: RegionModel): RegionModel? {
        val entityId: Long = targetModel.id.id

        return regionJpaRepository.findById(entityId)
            .map { existingEntity ->
                existingEntity.name = targetModel.name
                existingEntity.boundaryRepresentation = targetModel.boundaryRepresentation
                val continentEntity = continentJpaRepository.findById(targetModel.continent.id.id).orElseThrow()
                existingEntity.continent = continentEntity
                regionJpaRepository.save(existingEntity)
            }
            .map { geoLocationJpaMappers.regionToDomainModel(it) }
            .orElse(null)
    }

    override fun existsGeoLocationById(geoLocationId: GeoLocationId): Boolean {
        val entityIdToSearch: Long = geoLocationId.id
        return regionJpaRepository.existsById(entityIdToSearch)
    }

    override fun findById(geoLocationId: GeoLocationId): Optional<RegionModel> {
        return regionJpaRepository.findById(geoLocationId.id)
            .map { geoLocationJpaMappers.regionToDomainModel(it) }
    }

    override fun findAll(): List<RegionModel> {
        return regionJpaRepository.findAll()
            .map { geoLocationJpaMappers.regionToDomainModel(it) }
    }

    override fun deleteById(geoLocationId: GeoLocationId) {
        regionJpaRepository.deleteById(geoLocationId.id)
    }

    override fun deleteAll() {
        regionJpaRepository.deleteAll()
    }

    override fun findByContinentIdAndNameStartingWith(continentId: GeoLocationId, namePrefix: String): List<RegionModel> {
        return regionJpaRepository.findByContinentIdAndNameStartingWithIgnoreCase(continentId.id, namePrefix)
            .map { geoLocationJpaMappers.regionToDomainModel(it) }
    }

    override fun findByContinentIdAndAliasStartingWith(continentId: GeoLocationId, searchedAlias: String): List<RegionModel> {
        return regionJpaRepository.findByContinentIdAndAliasStartingWithIgnoreCase(continentId.id, searchedAlias)
            .map { geoLocationJpaMappers.regionToDomainModel(it) }
    }

    override fun findDeepById(geoLocationId: GeoLocationId): Optional<RegionModel> {
        return regionJpaRepository.findDeepById(geoLocationId.id)
            .map { deepGeoLocationJpaMappers.deepRegionToDomainModel(it, true) }
    }

    override fun findDeepByName(name: String): Optional<RegionModel> {
        return regionJpaRepository.findDeepByName(name)
            .map { deepGeoLocationJpaMappers.deepRegionToDomainModel(it, true) }
    }
}