package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.models.ProvinceModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.IProvinceRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.ProvinceJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import java.util.*


@Adapter
class ProvincePersistenceJpaAdapter(
    val provinceJpaRepository: ProvinceJpaRepository,
    val geoLocationJpaMapper: GeoLocationJpaMapper
) : IProvinceRepositoryPort {

    override fun saveNew(targetModel: ProvinceModel): ProvinceModel {
        val convertedEntity = geoLocationJpaMapper.provinceToJpaEntity(targetModel)
        val entitySaved = provinceJpaRepository.save(convertedEntity)
        return geoLocationJpaMapper.provinceToDomainModel(entitySaved)
    }

    override fun update(targetModel: ProvinceModel): ProvinceModel? {
        val entityId: Long = targetModel.id.id

        return provinceJpaRepository.findById(entityId)
            .map { geoLocationJpaMapper.provinceToJpaEntity(targetModel, it) }
            .map { provinceJpaRepository.save(it) }
            .map { geoLocationJpaMapper.provinceToDomainModel(it) }
            .orElse(null)
    }

    override fun existsGeoLocationById(geoLocationId: GeoLocationId): Boolean {
        val entityIdToSearch: Long = geoLocationId.id
        return provinceJpaRepository.existsById(entityIdToSearch)
    }

    override fun findById(geoLocationId: GeoLocationId): Optional<ProvinceModel> {
        return provinceJpaRepository.findById(geoLocationId.id)
            .map { geoLocationJpaMapper.provinceToDomainModel(it) }
    }

    override fun findAll(): List<ProvinceModel> {
        return provinceJpaRepository.findAll()
            .map { geoLocationJpaMapper.provinceToDomainModel(it) }
    }

    override fun deleteById(geoLocationId: GeoLocationId) {
        provinceJpaRepository.deleteById(geoLocationId.id)
    }

}
