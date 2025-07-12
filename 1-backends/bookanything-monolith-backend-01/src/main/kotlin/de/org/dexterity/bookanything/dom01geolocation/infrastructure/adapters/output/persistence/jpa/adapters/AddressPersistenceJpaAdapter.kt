package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.AddressModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.IAddressRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.AddressJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import java.util.*


@Adapter
class AddressPersistenceJpaAdapter(
    val addressJpaRepository: AddressJpaRepository,
    val geoLocationJpaMapper: GeoLocationJpaMapper
) : IAddressRepositoryPort {

    override fun saveNew(targetModel: AddressModel): AddressModel {
        val convertedEntity = geoLocationJpaMapper.addressToJpaEntity(targetModel)
        val entitySaved = addressJpaRepository.save(convertedEntity)
        return geoLocationJpaMapper.addressToDomainModel(entitySaved)
    }

    override fun update(targetModel: AddressModel): AddressModel? {
        val entityId: Long = targetModel.id.id

        return addressJpaRepository.findById(entityId)
            .map { geoLocationJpaMapper.addressToJpaEntity(targetModel, it) }
            .map { addressJpaRepository.save(it) }
            .map { geoLocationJpaMapper.addressToDomainModel(it) }
            .orElse(null)
    }

    override fun existsGeoLocationById(geoLocationId: GeoLocationId): Boolean {
        val entityIdToSearch: Long = geoLocationId.id
        return addressJpaRepository.existsById(entityIdToSearch)
    }

    override fun findById(geoLocationId: GeoLocationId): Optional<AddressModel> {
        return addressJpaRepository.findById(geoLocationId.id)
            .map { geoLocationJpaMapper.addressToDomainModel(it) }
    }

    override fun findAll(): List<AddressModel> {
        return addressJpaRepository.findAll()
            .map { geoLocationJpaMapper.addressToDomainModel(it) }
    }

    override fun deleteById(geoLocationId: GeoLocationId) {
        addressJpaRepository.deleteById(geoLocationId.id)
    }

}
