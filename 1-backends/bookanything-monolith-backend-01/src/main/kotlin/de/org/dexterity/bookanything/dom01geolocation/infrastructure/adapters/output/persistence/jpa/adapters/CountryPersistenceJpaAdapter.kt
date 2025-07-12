package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.CountryModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.ICountryRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.CountryJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import java.util.*


@Adapter
class CountryPersistenceJpaAdapter(
    val countryJpaRepository: CountryJpaRepository,
    val geoLocationJpaMapper: GeoLocationJpaMapper
) : ICountryRepositoryPort {

    override fun saveNew(targetModel: CountryModel): CountryModel {
        val convertedEntity = geoLocationJpaMapper.countryToJpaEntity(targetModel)
        val entitySaved = countryJpaRepository.save(convertedEntity)
        return geoLocationJpaMapper.countryToDomainModel(entitySaved)
    }

    override fun update(targetModel: CountryModel): CountryModel? {
        val entityId: Long = targetModel.id.id

        return countryJpaRepository.findById(entityId)
            .map { geoLocationJpaMapper.countryToJpaEntity(targetModel, it) }
            .map { countryJpaRepository.save(it) }
            .map { geoLocationJpaMapper.countryToDomainModel(it) }
            .orElse(null)
    }

    override fun existsGeoLocationById(geoLocationId: GeoLocationId): Boolean {
        val entityIdToSearch: Long = geoLocationId.id
        return countryJpaRepository.existsById(entityIdToSearch)
    }

    override fun findById(geoLocationId: GeoLocationId): Optional<CountryModel> {
        return countryJpaRepository.findById(geoLocationId.id)
            .map { geoLocationJpaMapper.countryToDomainModel(it) }
    }

    override fun findAll(): List<CountryModel> {
        return countryJpaRepository.findAll()
            .map { geoLocationJpaMapper.countryToDomainModel(it) }
    }

    override fun deleteById(geoLocationId: GeoLocationId) {
        countryJpaRepository.deleteById(geoLocationId.id)
    }

}
