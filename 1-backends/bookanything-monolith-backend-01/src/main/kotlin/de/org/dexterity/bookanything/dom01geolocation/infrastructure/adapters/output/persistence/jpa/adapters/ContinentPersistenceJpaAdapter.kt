package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.ContinentModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.IContinentRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.ContinentJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import java.util.*


@Adapter
class ContinentPersistenceJpaAdapter(
    val continentJpaRepository: ContinentJpaRepository,
    val geoLocationJpaMapper: GeoLocationJpaMapper
) : IContinentRepositoryPort {

    override fun saveNew(targetModel: ContinentModel): ContinentModel {
        val convertedEntity = geoLocationJpaMapper.continentToJpaEntity(targetModel)
        val entitySaved = continentJpaRepository.save(convertedEntity)
        return geoLocationJpaMapper.continentToDomainModel(entitySaved)
    }

    override fun update(targetModel: ContinentModel): ContinentModel? {
        val entityId: Long = targetModel.id.id

        return continentJpaRepository.findById(entityId)
            .map { existingEntity ->
                existingEntity.name = targetModel.name
                existingEntity.boundaryRepresentation = targetModel.boundaryRepresentation
                continentJpaRepository.save(existingEntity)
            }
            .map { geoLocationJpaMapper.continentToDomainModel(it) }
            .orElse(null)
    }

    override fun existsGeoLocationById(geoLocationId: GeoLocationId): Boolean {
        val entityIdToSearch: Long = geoLocationId.id
        return continentJpaRepository.existsById(entityIdToSearch)
    }

    override fun findById(geoLocationId: GeoLocationId): Optional<ContinentModel> {
        return continentJpaRepository.findById(geoLocationId.id)
            .map { geoLocationJpaMapper.continentToDomainModel(it) }
    }

    override fun findAll(): List<ContinentModel> {
        return continentJpaRepository.findAll()
            .map { geoLocationJpaMapper.continentToDomainModel(it) }
    }

    override fun deleteById(geoLocationId: GeoLocationId) {
        continentJpaRepository.deleteById(geoLocationId.id)
    }

    override fun findByNameStartingWith(namePrefix: String): List<ContinentModel> {
        return continentJpaRepository.findByNameStartingWithIgnoreCase(namePrefix)
            .map { geoLocationJpaMapper.continentToDomainModel(it) }
    }
}