package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.DistrictModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.IDistrictRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.DistrictEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.DeepGeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoLocationJpaMappers
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.CityJpaRepository
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.DistrictJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import org.locationtech.jts.geom.Geometry
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.*

@Adapter
class DistrictPersistenceJpaAdapter(
    val districtJpaRepository: DistrictJpaRepository,
    val cityJpaRepository: CityJpaRepository,
    val geoLocationJpaMappers: GeoLocationJpaMappers,
    val deepGeoLocationJpaMappers: DeepGeoLocationJpaMappers
) : IDistrictRepositoryPort {

    override fun saveNew(targetModel: DistrictModel): DistrictModel {
        val cityEntity = cityJpaRepository.findById(targetModel.city.id.id).orElseThrow()
        val districtEntity = DistrictEntity(
            friendlyId = targetModel.friendlyId,
            name = targetModel.name,
            alias = targetModel.alias,
            additionalDetailsMap = targetModel.additionalDetailsMap,
            boundaryRepresentation = targetModel.boundaryRepresentation,
            city = cityEntity
        )
        val entitySaved = districtJpaRepository.save(districtEntity)
        val savedModel = geoLocationJpaMappers.districtToDomainModel(entitySaved, true)
        return savedModel
    }

    override fun update(targetModel: DistrictModel): DistrictModel? {
        val entityId: Long = targetModel.id.id

        return districtJpaRepository.findById(entityId)
            .map { existingEntity ->
                existingEntity.name = targetModel.name
                existingEntity.friendlyId = targetModel.friendlyId
                existingEntity.alias = targetModel.alias
                existingEntity.boundaryRepresentation = targetModel.boundaryRepresentation
                existingEntity.additionalDetailsMap = targetModel.additionalDetailsMap
                val cityEntity = cityJpaRepository.findById(targetModel.city.id.id).orElseThrow()
                existingEntity.city = cityEntity
                val savedEntity = districtJpaRepository.save(existingEntity)
                val savedModel = geoLocationJpaMappers.districtToDomainModel(savedEntity, true)
                savedModel
            }
            .orElse(null)
    }

    override fun updateBoundary(id: GeoLocationId, boundary: Geometry): DistrictModel? {
        return districtJpaRepository.findById(id.id)
            .map { existingEntity ->
                existingEntity.boundaryRepresentation = boundary
                districtJpaRepository.save(existingEntity)
            }
            .map { geoLocationJpaMappers.districtToDomainModel(it, true) }
            .orElse(null)
    }

    override fun existsGeoLocationById(geoLocationId: GeoLocationId): Boolean {
        val entityIdToSearch: Long = geoLocationId.id
        return districtJpaRepository.existsById(entityIdToSearch)
    }

    override fun findById(geoLocationId: GeoLocationId): Optional<DistrictModel> {
        return districtJpaRepository.findById(geoLocationId.id)
            .map { geoLocationJpaMappers.districtToDomainModel(it, true) }
    }

    override fun findAll(pageable: Pageable): Page<DistrictModel> {
        return districtJpaRepository.findAll(pageable)
            .map { geoLocationJpaMappers.districtToDomainModel(it, true) }
    }

    override fun deleteById(geoLocationId: GeoLocationId) {
        districtJpaRepository.deleteById(geoLocationId.id)
    }

    override fun deleteAll() {
        districtJpaRepository.deleteAll()
    }

    override fun findAllByCityId(cityId: GeoLocationId, pageable: Pageable): Page<DistrictModel> {
        return districtJpaRepository.findAllByCityId(cityId.id, pageable)
            .map { geoLocationJpaMappers.districtToDomainModel(it, true) }
    }

    override fun findByCityIdAndNameStartingWith(cityId: GeoLocationId, namePrefix: String, pageable: Pageable): Page<DistrictModel> {
        return districtJpaRepository.findByCityIdAndNameStartingWithIgnoreCase(cityId.id, namePrefix, pageable)
            .map { geoLocationJpaMappers.districtToDomainModel(it, true) }
    }

    override fun findByCityIdAndAliasStartingWith(cityId: GeoLocationId, searchedAlias: String, pageable: Pageable): Page<DistrictModel> {
        return districtJpaRepository.findByCityIdAndAliasStartingWithIgnoreCase(cityId.id, searchedAlias, pageable)
            .map { geoLocationJpaMappers.districtToDomainModel(it, true) }
    }

    override fun findDeepById(geoLocationId: GeoLocationId): Optional<DistrictModel> {
        return districtJpaRepository.findDeepById(geoLocationId.id)
            .map { deepGeoLocationJpaMappers.deepDistrictToDomainModel(it, true) }
    }

    override fun findDeepByName(name: String): Optional<DistrictModel> {
        return districtJpaRepository.findDeepByName(name)
            .map { deepGeoLocationJpaMappers.deepDistrictToDomainModel(it, true) }
    }

    override fun findByFriendlyIdContainingIgnoreCase(friendlyId: String, pageable: Pageable): Page<DistrictModel> {
        return districtJpaRepository.findByFriendlyIdContainingIgnoreCase(friendlyId, pageable)
            .map { geoLocationJpaMappers.districtToDomainModel(it, true) }
    }

    override fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<DistrictModel> {
        return districtJpaRepository.findByPropertiesDetailsMapContains(key, value, pageable)
            .map { geoLocationJpaMappers.districtToDomainModel(it, true) }
    }
}