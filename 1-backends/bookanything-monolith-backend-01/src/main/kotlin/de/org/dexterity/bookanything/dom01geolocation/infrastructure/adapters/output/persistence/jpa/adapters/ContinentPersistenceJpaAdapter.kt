package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.ContinentModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.IContinentRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.DeepGeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.ContinentJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import org.locationtech.jts.geom.Geometry
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.*

@Adapter
class ContinentPersistenceJpaAdapter(
    val continentJpaRepository: ContinentJpaRepository,
    val geoLocationJpaMappers: GeoLocationJpaMappers,
    val deepGeoLocationJpaMappers: DeepGeoLocationJpaMappers
) : IContinentRepositoryPort {

    override fun saveNew(targetModel: ContinentModel): ContinentModel {
        val convertedEntity = geoLocationJpaMappers.continentToJpaEntity(targetModel)
        val entitySaved = continentJpaRepository.save(convertedEntity)
        val savedModel = geoLocationJpaMappers.continentToDomainModel(entitySaved, true)
        return savedModel
    }

    override fun update(targetModel: ContinentModel): ContinentModel? {
        val entityId: Long = targetModel.id.id

        return continentJpaRepository.findById(entityId)
            .map { existingEntity ->
                existingEntity.name = targetModel.name
                existingEntity.friendlyId = targetModel.friendlyId
                existingEntity.alias = targetModel.alias
                existingEntity.boundaryRepresentation = targetModel.boundaryRepresentation
                existingEntity.additionalDetailsMap = targetModel.additionalDetailsMap
                val savedEntity = continentJpaRepository.save(existingEntity)
                val savedModel = geoLocationJpaMappers.continentToDomainModel(savedEntity, true)
                savedModel
            }
            .orElse(null)
    }

    override fun updateBoundary(id: GeoLocationId, boundary: Geometry): ContinentModel? {
        return continentJpaRepository.findById(id.id)
            .map { existingEntity ->
                existingEntity.boundaryRepresentation = boundary
                continentJpaRepository.save(existingEntity)
            }
            .map { geoLocationJpaMappers.continentToDomainModel(it, true) }
            .orElse(null)
    }

    override fun existsGeoLocationById(geoLocationId: GeoLocationId): Boolean {
        val entityIdToSearch: Long = geoLocationId.id
        return continentJpaRepository.existsById(entityIdToSearch)
    }

    override fun findById(geoLocationId: GeoLocationId): Optional<ContinentModel> {
        return continentJpaRepository.findById(geoLocationId.id)
            .map { geoLocationJpaMappers.continentToDomainModel(it, true) }
    }

    override fun findAll(pageable: Pageable): Page<ContinentModel> {
        return continentJpaRepository.findAll(pageable)
            .map { geoLocationJpaMappers.continentToDomainModel(it, true) }
    }

    override fun deleteById(geoLocationId: GeoLocationId) {
        continentJpaRepository.deleteById(geoLocationId.id)
    }

    override fun deleteAll() {
        continentJpaRepository.deleteAll()
    }

    override fun findByNameStartingWith(
        namePrefix: String,
        pageable: Pageable
    ): Page<ContinentModel> {

        return continentJpaRepository.findByNameStartingWithIgnoreCase(namePrefix, pageable)
            .map { geoLocationJpaMappers.continentToDomainModel(it, true) }
    }

    override fun findByAliasStartingWith(
        searchedAlias: String,
        pageable: Pageable
    ): Page<ContinentModel> {

        return continentJpaRepository.findByAliasStartingWithIgnoreCase(searchedAlias, pageable)
            .map { geoLocationJpaMappers.continentToDomainModel(it, true) }
    }

    override fun findByFriendlyIdContainingIgnoreCase(friendlyId: String, pageable: Pageable): Page<ContinentModel> {
        return continentJpaRepository.findByFriendlyIdContainingIgnoreCase(friendlyId, pageable)
            .map { geoLocationJpaMappers.continentToDomainModel(it, true) }
    }

    override fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<ContinentModel> {
        return continentJpaRepository.findByPropertiesDetailsMapContains(key, value, pageable)
            .map { geoLocationJpaMappers.continentToDomainModel(it, true) }
    }

    override fun findDeepById(geoLocationId: GeoLocationId): Optional<ContinentModel> {
        return continentJpaRepository.findDeepById(geoLocationId.id)
            .map { deepGeoLocationJpaMappers.deepContinentToDomainModel(it, true) }
    }

    override fun findDeepByName(name: String): Optional<ContinentModel> {
        return continentJpaRepository.findDeepByName(name)
            .map { deepGeoLocationJpaMappers.deepContinentToDomainModel(it, true) }
    }

}