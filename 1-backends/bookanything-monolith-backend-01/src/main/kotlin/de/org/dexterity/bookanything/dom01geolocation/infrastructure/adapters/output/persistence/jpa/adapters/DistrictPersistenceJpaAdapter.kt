package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.DistrictModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.IDistrictRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.DistrictEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoLocationJpaMapper
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.CityJpaRepository
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.DistrictJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import java.util.*

@Adapter
class DistrictPersistenceJpaAdapter(
    val districtJpaRepository: DistrictJpaRepository,
    val cityJpaRepository: CityJpaRepository,
    val geoLocationJpaMapper: GeoLocationJpaMapper
) : IDistrictRepositoryPort {

    override fun saveNew(targetModel: DistrictModel): DistrictModel {
        val cityEntity = cityJpaRepository.findById(targetModel.city.id.id).orElseThrow()
        val districtEntity = DistrictEntity(
            name = targetModel.name,
            boundaryRepresentation = targetModel.boundaryRepresentation,
            city = cityEntity
        )
        val entitySaved = districtJpaRepository.save(districtEntity)
        return geoLocationJpaMapper.districtToDomainModel(entitySaved)
    }

    override fun update(targetModel: DistrictModel): DistrictModel? {
        val entityId: Long = targetModel.id.id

        return districtJpaRepository.findById(entityId)
            .map { existingEntity ->
                existingEntity.name = targetModel.name
                existingEntity.boundaryRepresentation = targetModel.boundaryRepresentation
                val cityEntity = cityJpaRepository.findById(targetModel.city.id.id).orElseThrow()
                existingEntity.city = cityEntity
                districtJpaRepository.save(existingEntity)
            }
            .map { geoLocationJpaMapper.districtToDomainModel(it) }
            .orElse(null)
    }

    override fun existsGeoLocationById(geoLocationId: GeoLocationId): Boolean {
        val entityIdToSearch: Long = geoLocationId.id
        return districtJpaRepository.existsById(entityIdToSearch)
    }

    override fun findById(geoLocationId: GeoLocationId): Optional<DistrictModel> {
        return districtJpaRepository.findById(geoLocationId.id)
            .map { geoLocationJpaMapper.districtToDomainModel(it) }
    }

    override fun findAll(): List<DistrictModel> {
        return districtJpaRepository.findAll()
            .map { geoLocationJpaMapper.districtToDomainModel(it) }
    }

    override fun deleteById(geoLocationId: GeoLocationId) {
        districtJpaRepository.deleteById(geoLocationId.id)
    }

    override fun findByCityIdAndNameStartingWith(cityId: GeoLocationId, namePrefix: String): List<DistrictModel> {
        return districtJpaRepository.findByCityIdAndNameStartingWithIgnoreCase(cityId.id, namePrefix)
            .map { geoLocationJpaMapper.districtToDomainModel(it) }
    }
}