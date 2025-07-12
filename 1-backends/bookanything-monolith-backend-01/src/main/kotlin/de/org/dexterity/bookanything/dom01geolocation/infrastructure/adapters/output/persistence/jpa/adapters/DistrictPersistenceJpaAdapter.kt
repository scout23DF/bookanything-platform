package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.ports.IDistrictRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.DistrictJpaRepository
import de.org.dexterity.bookanything.dom01geolocation.domain.models.DistrictModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.shared.annotations.Adapter
import java.util.*


@Adapter
class DistrictPersistenceJpaAdapter(
    val districtJpaRepository: DistrictJpaRepository,
    val geoLocationJpaMapper: GeoLocationJpaMapper
) : IDistrictRepositoryPort {

    override fun saveNew(targetModel: DistrictModel): DistrictModel {
        val convertedEntity = geoLocationJpaMapper.districtToJpaEntity(targetModel)
        val entitySaved = districtJpaRepository.save(convertedEntity)
        return geoLocationJpaMapper.districtToDomainModel(entitySaved)
    }

    override fun update(targetModel: DistrictModel): DistrictModel? {
        val entityId: Long = targetModel.id.id

        return districtJpaRepository.findById(entityId)
            .map { geoLocationJpaMapper.districtToJpaEntity(targetModel, it) }
            .map { districtJpaRepository.save(it) }
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

}
