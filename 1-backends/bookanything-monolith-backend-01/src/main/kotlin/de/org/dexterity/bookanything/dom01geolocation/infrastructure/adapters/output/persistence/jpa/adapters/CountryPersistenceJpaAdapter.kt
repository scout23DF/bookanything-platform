package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.CountryModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.ICountryRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.CountryEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.DeepGeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.CountryJpaRepository
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.RegionJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import java.util.*

@Adapter
class CountryPersistenceJpaAdapter(
    val countryJpaRepository: CountryJpaRepository,
    val regionJpaRepository: RegionJpaRepository,
    val geoLocationJpaMappers: GeoLocationJpaMappers,
    val deepGeoLocationJpaMappers: DeepGeoLocationJpaMappers
) : ICountryRepositoryPort {

    override fun saveNew(targetModel: CountryModel): CountryModel {
        val regionEntity = regionJpaRepository.findById(targetModel.region.id.id).orElseThrow()
        val countryEntity = CountryEntity(
            name = targetModel.name,
            boundaryRepresentation = targetModel.boundaryRepresentation,
            region = regionEntity
        )
        val entitySaved = countryJpaRepository.save(countryEntity)
        return geoLocationJpaMappers.countryToDomainModel(entitySaved)
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
            .map { geoLocationJpaMappers.countryToDomainModel(it) }
            .orElse(null)
    }

    override fun existsGeoLocationById(geoLocationId: GeoLocationId): Boolean {
        val entityIdToSearch: Long = geoLocationId.id
        return countryJpaRepository.existsById(entityIdToSearch)
    }

    override fun findById(geoLocationId: GeoLocationId): Optional<CountryModel> {
        return countryJpaRepository.findById(geoLocationId.id)
            .map { geoLocationJpaMappers.countryToDomainModel(it) }
    }

    override fun findAll(): List<CountryModel> {
        return countryJpaRepository.findAll()
            .map { geoLocationJpaMappers.countryToDomainModel(it) }
    }

    override fun deleteById(geoLocationId: GeoLocationId) {
        countryJpaRepository.deleteById(geoLocationId.id)
    }

    override fun deleteAll() {
        countryJpaRepository.deleteAll()
    }

    override fun findByRegionIdAndNameStartingWith(regionId: GeoLocationId, namePrefix: String): List<CountryModel> {
        return countryJpaRepository.findByRegionIdAndNameStartingWithIgnoreCase(regionId.id, namePrefix)
            .map { geoLocationJpaMappers.countryToDomainModel(it) }
    }

    override fun findByRegionIdAndAliasStartingWith(regionId: GeoLocationId, searchedAlias: String): List<CountryModel> {
        return countryJpaRepository.findByRegionIdAndAliasStartingWithIgnoreCase(regionId.id, searchedAlias)
            .map { geoLocationJpaMappers.countryToDomainModel(it) }
    }

    override fun findDeepById(geoLocationId: GeoLocationId): Optional<CountryModel> {
        return countryJpaRepository.findDeepById(geoLocationId.id)
            .map { deepGeoLocationJpaMappers.deepCountryToDomainModel(it, true) }
    }

    override fun findDeepByName(name: String): Optional<CountryModel> {
        return countryJpaRepository.findDeepByName(name)
            .map { deepGeoLocationJpaMappers.deepCountryToDomainModel(it, true) }
    }
}