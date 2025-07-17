package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.CountryModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.ICountryRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.CountryEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoLocationJpaMapper
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.CountryJpaRepository
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.RegionJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import java.util.*

@Adapter
class CountryPersistenceJpaAdapter(
    val countryJpaRepository: CountryJpaRepository,
    val regionJpaRepository: RegionJpaRepository,
    val geoLocationJpaMapper: GeoLocationJpaMapper
) : ICountryRepositoryPort {

    override fun saveNew(targetModel: CountryModel): CountryModel {
        val regionEntity = regionJpaRepository.findById(targetModel.region.id.id).orElseThrow()
        val countryEntity = CountryEntity(
            name = targetModel.name,
            boundaryRepresentation = targetModel.boundaryRepresentation,
            region = regionEntity
        )
        val entitySaved = countryJpaRepository.save(countryEntity)
        return geoLocationJpaMapper.countryToDomainModel(entitySaved)
    }

    override fun update(targetModel: CountryModel): CountryModel? {
        val entityId: Long = targetModel.id.id

        return countryJpaRepository.findById(entityId)
            .map { existingEntity ->
                existingEntity.name = targetModel.name
                existingEntity.boundaryRepresentation = targetModel.boundaryRepresentation
                val regionEntity = regionJpaRepository.findById(targetModel.region.id.id).orElseThrow()
                existingEntity.region = regionEntity
                countryJpaRepository.save(existingEntity)
            }
            .map { geoLocationJpaMapper.countryToDomainModel(it) }
            .orElse(null)
    }

    override fun existsGeoLocationById(geoLocationId: GeoLocationId): Boolean {
        val entityIdToSearch: Long = geoLocationId.id
        return countryJpaRepository.existsById(entityIdToSearch)
    }

    override fun findById(geoLocationId: GeoLocationId): Optional<CountryModel> {
        return countryJpaRepository.findById(geoLocationId.id)
            .map { geoLocationJpaMapper.countryToDomainModel(it) }
    }

    override fun findAll(): List<CountryModel> {
        return countryJpaRepository.findAll()
            .map { geoLocationJpaMapper.countryToDomainModel(it) }
    }

    override fun deleteById(geoLocationId: GeoLocationId) {
        countryJpaRepository.deleteById(geoLocationId.id)
    }

    override fun deleteAll() {
        countryJpaRepository.deleteAll()
    }

    override fun findByRegionIdAndNameStartingWith(regionId: GeoLocationId, namePrefix: String): List<CountryModel> {
        return countryJpaRepository.findByRegionIdAndNameStartingWithIgnoreCase(regionId.id, namePrefix)
            .map { geoLocationJpaMapper.countryToDomainModel(it) }
    }

    override fun findByRegionIdAndAliasStartingWith(regionId: GeoLocationId, searchedAlias: String): List<CountryModel> {
        return countryJpaRepository.findByRegionIdAndAliasStartingWithIgnoreCase(regionId.id, searchedAlias)
            .map { geoLocationJpaMapper.countryToDomainModel(it) }
    }
}