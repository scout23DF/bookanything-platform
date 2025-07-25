package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.CityModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.ICityRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.CityEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.DeepGeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.CityJpaRepository
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.ProvinceJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import org.locationtech.jts.geom.Geometry
import java.util.*

@Adapter
class CityPersistenceJpaAdapter(
    val cityJpaRepository: CityJpaRepository,
    val provinceJpaRepository: ProvinceJpaRepository,
    val geoLocationJpaMappers: GeoLocationJpaMappers,
    val deepGeoLocationJpaMappers: DeepGeoLocationJpaMappers
) : ICityRepositoryPort {

    override fun saveNew(targetModel: CityModel): CityModel {
        val provinceEntity = provinceJpaRepository.findById(targetModel.province.id.id).orElseThrow()
        val cityEntity = CityEntity(
            friendlyId = targetModel.friendlyId,
            name = targetModel.name,
            alias = targetModel.alias,
            additionalDetailsMap = targetModel.additionalDetailsMap,
            boundaryRepresentation = targetModel.boundaryRepresentation,
            province = provinceEntity
        )
        val entitySaved = cityJpaRepository.save(cityEntity)
        val savedModel = geoLocationJpaMappers.cityToDomainModel(entitySaved, true)
        return savedModel
    }

    override fun update(targetModel: CityModel): CityModel? {
        val entityId: Long = targetModel.id.id

        return cityJpaRepository.findById(entityId)
            .map { existingEntity ->
                existingEntity.name = targetModel.name
                existingEntity.friendlyId = targetModel.friendlyId
                existingEntity.alias = targetModel.alias
                existingEntity.boundaryRepresentation = targetModel.boundaryRepresentation
                existingEntity.additionalDetailsMap = targetModel.additionalDetailsMap
                val provinceEntity = provinceJpaRepository.findById(targetModel.province.id.id).orElseThrow()
                existingEntity.province = provinceEntity
                val savedEntity = cityJpaRepository.save(existingEntity)
                val savedModel = geoLocationJpaMappers.cityToDomainModel(savedEntity, true)
                savedModel
            }
            .orElse(null)
    }

    override fun updateBoundary(id: GeoLocationId, boundary: Geometry): CityModel? {
        return cityJpaRepository.findById(id.id)
            .map { existingEntity ->
                existingEntity.boundaryRepresentation = boundary
                cityJpaRepository.save(existingEntity)
            }
            .map { geoLocationJpaMappers.cityToDomainModel(it, true) }
            .orElse(null)
    }

    override fun existsGeoLocationById(geoLocationId: GeoLocationId): Boolean {
        val entityIdToSearch: Long = geoLocationId.id
        return cityJpaRepository.existsById(entityIdToSearch)
    }

    override fun findById(geoLocationId: GeoLocationId): Optional<CityModel> {
        return cityJpaRepository.findById(geoLocationId.id)
            .map { geoLocationJpaMappers.cityToDomainModel(it, true) }
    }

    override fun findAll(): List<CityModel> {
        return cityJpaRepository.findAll()
            .map { geoLocationJpaMappers.cityToDomainModel(it, true) }
    }

    override fun deleteById(geoLocationId: GeoLocationId) {
        cityJpaRepository.deleteById(geoLocationId.id)
    }

    override fun deleteAll() {
        cityJpaRepository.deleteAll()
    }

    override fun findByProvinceIdAndNameStartingWith(provinceId: GeoLocationId, namePrefix: String): List<CityModel> {
        return cityJpaRepository.findByProvinceIdAndNameStartingWithIgnoreCase(provinceId.id, namePrefix)
            .map { geoLocationJpaMappers.cityToDomainModel(it, true) }
    }

    override fun findByProvinceIdAndAliasStartingWith(provinceId: GeoLocationId, searchedAlias: String): List<CityModel> {
        return cityJpaRepository.findByProvinceIdAndAliasStartingWithIgnoreCase(provinceId.id, searchedAlias)
            .map { geoLocationJpaMappers.cityToDomainModel(it, true) }
    }

    override fun findDeepById(geoLocationId: GeoLocationId): Optional<CityModel> {
        return cityJpaRepository.findDeepById(geoLocationId.id)
            .map { deepGeoLocationJpaMappers.deepCityToDomainModel(it, true) }
    }

    override fun findDeepByName(name: String): Optional<CityModel> {
        return cityJpaRepository.findDeepByName(name)
            .map { deepGeoLocationJpaMappers.deepCityToDomainModel(it, true) }
    }

    override fun findByFriendlyIdContainingIgnoreCase(friendlyId: String): List<CityModel> {
        return cityJpaRepository.findByFriendlyIdContainingIgnoreCase(friendlyId)
            .map { geoLocationJpaMappers.cityToDomainModel(it, true) }
    }

    override fun findByPropertiesDetailsMapContains(key: String, value: String): List<CityModel> {
        return cityJpaRepository.findByPropertiesDetailsMapContains(key, value)
            .map { geoLocationJpaMappers.cityToDomainModel(it, true) }
    }
}