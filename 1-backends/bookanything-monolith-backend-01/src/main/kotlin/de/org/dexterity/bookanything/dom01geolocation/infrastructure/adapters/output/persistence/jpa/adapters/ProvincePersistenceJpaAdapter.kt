package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.models.ProvinceModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.IProvinceRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.ProvinceEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.GeoLocationJpaMapper
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.CountryJpaRepository
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.ProvinceJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import java.util.*

@Adapter
class ProvincePersistenceJpaAdapter(
    val provinceJpaRepository: ProvinceJpaRepository,
    val countryJpaRepository: CountryJpaRepository,
    val geoLocationJpaMapper: GeoLocationJpaMapper
) : IProvinceRepositoryPort {

    override fun saveNew(targetModel: ProvinceModel): ProvinceModel {
        val countryEntity = countryJpaRepository.findById(targetModel.country.id.id).orElseThrow()
        val provinceEntity = ProvinceEntity(
            name = targetModel.name,
            boundaryRepresentation = targetModel.boundaryRepresentation,
            country = countryEntity
        )
        val entitySaved = provinceJpaRepository.save(provinceEntity)
        return geoLocationJpaMapper.provinceToDomainModel(entitySaved)
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

    override fun deleteAll() {
        provinceJpaRepository.deleteAll()
    }

    override fun findByCountryIdAndNameStartingWith(countryId: GeoLocationId, namePrefix: String): List<ProvinceModel> {
        return provinceJpaRepository.findByCountryIdAndNameStartingWithIgnoreCase(countryId.id, namePrefix)
            .map { geoLocationJpaMapper.provinceToDomainModel(it) }
    }
}