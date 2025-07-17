package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.models.ProvinceModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.IProvinceRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.ProvinceEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.DeepGeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.CountryJpaRepository
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.ProvinceJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import java.util.*

@Adapter
class ProvincePersistenceJpaAdapter(
    val provinceJpaRepository: ProvinceJpaRepository,
    val countryJpaRepository: CountryJpaRepository,
    val geoLocationJpaMappers: GeoLocationJpaMappers,
    val deepGeoLocationJpaMappers: DeepGeoLocationJpaMappers
) : IProvinceRepositoryPort {

    override fun saveNew(targetModel: ProvinceModel): ProvinceModel {
        val countryEntity = countryJpaRepository.findById(targetModel.country.id.id).orElseThrow()
        val provinceEntity = ProvinceEntity(
            name = targetModel.name,
            boundaryRepresentation = targetModel.boundaryRepresentation,
            country = countryEntity
        )
        val entitySaved = provinceJpaRepository.save(provinceEntity)
        return geoLocationJpaMappers.provinceToDomainModel(entitySaved)
    }

    override fun update(targetModel: ProvinceModel): ProvinceModel? {
        val entityId: Long = targetModel.id.id

        return provinceJpaRepository.findById(entityId)
            .map { existingEntity ->
                existingEntity.name = targetModel.name
                existingEntity.boundaryRepresentation = targetModel.boundaryRepresentation
                val countryEntity = countryJpaRepository.findById(targetModel.country.id.id).orElseThrow()
                existingEntity.country = countryEntity
                provinceJpaRepository.save(existingEntity)
            }
            .map { geoLocationJpaMappers.provinceToDomainModel(it) }
            .orElse(null)
    }

    override fun existsGeoLocationById(geoLocationId: GeoLocationId): Boolean {
        val entityIdToSearch: Long = geoLocationId.id
        return provinceJpaRepository.existsById(entityIdToSearch)
    }

    override fun findById(geoLocationId: GeoLocationId): Optional<ProvinceModel> {
        return provinceJpaRepository.findById(geoLocationId.id)
            .map { geoLocationJpaMappers.provinceToDomainModel(it) }
    }

    override fun findAll(): List<ProvinceModel> {
        return provinceJpaRepository.findAll()
            .map { geoLocationJpaMappers.provinceToDomainModel(it) }
    }

    override fun deleteById(geoLocationId: GeoLocationId) {
        provinceJpaRepository.deleteById(geoLocationId.id)
    }

    override fun deleteAll() {
        provinceJpaRepository.deleteAll()
    }

    override fun findByCountryIdAndNameStartingWith(countryId: GeoLocationId, namePrefix: String): List<ProvinceModel> {
        return provinceJpaRepository.findByCountryIdAndNameStartingWithIgnoreCase(countryId.id, namePrefix)
            .map { geoLocationJpaMappers.provinceToDomainModel(it) }
    }

    override fun findByCountryIdAndAliasStartingWith(countryId: GeoLocationId, searchedAlias: String): List<ProvinceModel> {
        return provinceJpaRepository.findByCountryIdAndAliasStartingWithIgnoreCase(countryId.id, searchedAlias)
            .map { geoLocationJpaMappers.provinceToDomainModel(it) }
    }

    override fun findDeepById(geoLocationId: GeoLocationId): Optional<ProvinceModel> {
        return provinceJpaRepository.findDeepById(geoLocationId.id)
            .map { deepGeoLocationJpaMappers.deepProvinceToDomainModel(it, true) }
    }

    override fun findDeepByName(name: String): Optional<ProvinceModel> {
        return provinceJpaRepository.findDeepByName(name)
            .map { deepGeoLocationJpaMappers.deepProvinceToDomainModel(it, true) }
    }
}