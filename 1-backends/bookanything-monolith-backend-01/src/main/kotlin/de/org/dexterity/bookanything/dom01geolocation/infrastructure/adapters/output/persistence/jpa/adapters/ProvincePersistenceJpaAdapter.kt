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
import org.locationtech.jts.geom.Geometry
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
            friendlyId = targetModel.friendlyId,
            name = targetModel.name,
            alias = targetModel.alias,
            additionalDetailsMap = targetModel.additionalDetailsMap,
            boundaryRepresentation = targetModel.boundaryRepresentation,
            country = countryEntity
        )
        val entitySaved = provinceJpaRepository.save(provinceEntity)
        val savedModel = geoLocationJpaMappers.provinceToDomainModel(entitySaved, true)
        return savedModel
    }

    override fun update(targetModel: ProvinceModel): ProvinceModel? {
        val entityId: Long = targetModel.id.id

        return provinceJpaRepository.findById(entityId)
            .map { existingEntity ->
                existingEntity.name = targetModel.name
                existingEntity.friendlyId = targetModel.friendlyId
                existingEntity.alias = targetModel.alias
                existingEntity.boundaryRepresentation = targetModel.boundaryRepresentation
                existingEntity.additionalDetailsMap = targetModel.additionalDetailsMap
                val countryEntity = countryJpaRepository.findById(targetModel.country.id.id).orElseThrow()
                existingEntity.country = countryEntity
                val savedEntity = provinceJpaRepository.save(existingEntity)
                val savedModel = geoLocationJpaMappers.provinceToDomainModel(savedEntity, true)
                savedModel
            }
            .orElse(null)
    }

    override fun updateBoundary(id: GeoLocationId, boundary: Geometry): ProvinceModel? {
        return provinceJpaRepository.findById(id.id)
            .map { existingEntity ->
                existingEntity.boundaryRepresentation = boundary
                provinceJpaRepository.save(existingEntity)
            }
            .map { geoLocationJpaMappers.provinceToDomainModel(it, true) }
            .orElse(null)
    }

    override fun existsGeoLocationById(geoLocationId: GeoLocationId): Boolean {
        val entityIdToSearch: Long = geoLocationId.id
        return provinceJpaRepository.existsById(entityIdToSearch)
    }

    override fun findById(geoLocationId: GeoLocationId): Optional<ProvinceModel> {
        return provinceJpaRepository.findById(geoLocationId.id)
            .map { geoLocationJpaMappers.provinceToDomainModel(it, true) }
    }

    override fun findAll(pageable: Pageable): Page<ProvinceModel> {
        return provinceJpaRepository.findAll(pageable)
            .map { geoLocationJpaMappers.provinceToDomainModel(it, true) }
    }

    override fun deleteById(geoLocationId: GeoLocationId) {
        provinceJpaRepository.deleteById(geoLocationId.id)
    }

    override fun deleteAll() {
        provinceJpaRepository.deleteAll()
    }

    override fun findAllByCountryId(countryId: GeoLocationId, pageable: Pageable): Page<ProvinceModel> {
        return provinceJpaRepository.findAllByCountryId(countryId.id, pageable)
            .map { geoLocationJpaMappers.provinceToDomainModel(it, true) }
    }

    override fun findByCountryIdAndNameStartingWith(countryId: GeoLocationId, namePrefix: String, pageable: Pageable): Page<ProvinceModel> {
        return provinceJpaRepository.findByCountryIdAndNameStartingWithIgnoreCase(countryId.id, namePrefix, pageable)
            .map { geoLocationJpaMappers.provinceToDomainModel(it, true) }
    }

    override fun findByCountryIdAndAliasStartingWith(countryId: GeoLocationId, searchedAlias: String, pageable: Pageable): Page<ProvinceModel> {
        return provinceJpaRepository.findByCountryIdAndAliasStartingWithIgnoreCase(countryId.id, searchedAlias, pageable)
            .map { geoLocationJpaMappers.provinceToDomainModel(it, true) }
    }

    override fun findDeepById(geoLocationId: GeoLocationId): Optional<ProvinceModel> {
        return provinceJpaRepository.findDeepById(geoLocationId.id)
            .map { deepGeoLocationJpaMappers.deepProvinceToDomainModel(it, true) }
    }

    override fun findDeepByName(name: String): Optional<ProvinceModel> {
        return provinceJpaRepository.findDeepByName(name)
            .map { deepGeoLocationJpaMappers.deepProvinceToDomainModel(it, true) }
    }

    // @Cacheable("provincesByFriendlyId")
    override fun findByFriendlyIdContainingIgnoreCase(friendlyId: String, pageable: Pageable): Page<ProvinceModel> {
        return provinceJpaRepository.findByFriendlyIdContainingIgnoreCase(friendlyId, pageable)
            .map { geoLocationJpaMappers.provinceToDomainModel(it, true) }
    }

    // @Cacheable("provincesByPropertiesMap")
    override fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<ProvinceModel> {
        return provinceJpaRepository.findByPropertiesDetailsMapContains(key, value, pageable)
            .map { geoLocationJpaMappers.provinceToDomainModel(it, true) }
    }
}