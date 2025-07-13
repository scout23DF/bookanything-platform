package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.CityModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.ICityRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.CityJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import java.util.*


@Adapter
class CityPersistenceJpaAdapter(
    val cityJpaRepository: CityJpaRepository,
    val geoLocationJpaMapper: GeoLocationJpaMapper
) : ICityRepositoryPort {

    override fun saveNew(targetModel: CityModel): CityModel {
        val convertedEntity = geoLocationJpaMapper.cityToJpaEntity(targetModel)
        val entitySaved = cityJpaRepository.save(convertedEntity)
        return geoLocationJpaMapper.cityToDomainModel(entitySaved)
    }

    override fun update(targetModel: CityModel): CityModel? {
        val entityId: Long = targetModel.id.id

        return cityJpaRepository.findById(entityId)
            .map { geoLocationJpaMapper.cityToJpaEntity(targetModel) }
            .map { cityJpaRepository.save(it) }
            .map { geoLocationJpaMapper.cityToDomainModel(it) }
            .orElse(null)
    }

    override fun existsGeoLocationById(geoLocationId: GeoLocationId): Boolean {
        val entityIdToSearch: Long = geoLocationId.id
        return cityJpaRepository.existsById(entityIdToSearch)
    }

    override fun findById(geoLocationId: GeoLocationId): Optional<CityModel> {
        return cityJpaRepository.findById(geoLocationId.id)
            .map { geoLocationJpaMapper.cityToDomainModel(it) }
    }

    override fun findAll(): List<CityModel> {
        return cityJpaRepository.findAll()
            .map { geoLocationJpaMapper.cityToDomainModel(it) }
    }

    override fun deleteById(geoLocationId: GeoLocationId) {
        cityJpaRepository.deleteById(geoLocationId.id)
    }

}
