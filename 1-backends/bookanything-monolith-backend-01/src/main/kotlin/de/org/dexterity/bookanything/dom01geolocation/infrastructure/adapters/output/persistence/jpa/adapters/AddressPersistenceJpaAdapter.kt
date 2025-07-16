package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.AddressModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.AddressPersistRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.AddressEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.mappers.AddressJpaMapper
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.AddressJpaRepository
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories.DistrictJpaRepository
import de.org.dexterity.bookanything.shared.annotations.Adapter
import java.util.*

@Adapter
class AddressPersistenceJpaAdapter(
    val addressJpaRepository: AddressJpaRepository,
    val districtJpaRepository: DistrictJpaRepository,
    val addressJpaMapper: AddressJpaMapper
) : AddressPersistRepositoryPort {

    override fun saveNew(targetModel: AddressModel): AddressModel {
        val districtEntity = districtJpaRepository.findById(targetModel.district.id.id).orElseThrow()

        val newAddressEntity = AddressEntity(
            streetName = targetModel.streetName,
            houseNumber = targetModel.houseNumber,
            floorNumber = targetModel.floorNumber,
            doorNumber = targetModel.doorNumber,
            addressLine2 = targetModel.addressLine2,
            postalCode = targetModel.postalCode,
            districtName = targetModel.districtName,
            cityName = targetModel.cityName,
            provinceName = targetModel.provinceName,
            countryName = targetModel.countryName,
            coordinates = addressJpaMapper.buildPointFromGeoCoordinate(targetModel.coordinates),
            district = districtEntity
        )

        val entitySaved = addressJpaRepository.save(newAddressEntity)

        return addressJpaMapper.addressToDomainModel(entitySaved)
    }

    override fun update(targetModel: AddressModel): AddressModel? {
        val entityId: Long = targetModel.id.id

        return addressJpaRepository.findById(entityId)
            .map { existingEntity ->
                existingEntity.streetName = targetModel.streetName
                existingEntity.houseNumber = targetModel.houseNumber
                existingEntity.floorNumber = targetModel.floorNumber
                existingEntity.doorNumber = targetModel.doorNumber
                existingEntity.addressLine2 = targetModel.addressLine2
                existingEntity.postalCode = targetModel.postalCode
                existingEntity.districtName = targetModel.districtName
                existingEntity.cityName = targetModel.cityName
                existingEntity.provinceName = targetModel.provinceName
                existingEntity.countryName = targetModel.countryName
                existingEntity.coordinates = addressJpaMapper.buildPointFromGeoCoordinate(targetModel.coordinates)
                existingEntity.status = targetModel.status
                val districtEntity = districtJpaRepository.findById(targetModel.district.id.id).orElseThrow()
                existingEntity.district = districtEntity
                addressJpaRepository.save(existingEntity)
            }
            .map { addressJpaMapper.addressToDomainModel(it) }
            .orElse(null)
    }

    override fun existsAddressById(geoLocationId: GeoLocationId): Boolean {
        val entityIdToSearch: Long = geoLocationId.id
        return addressJpaRepository.existsById(entityIdToSearch)
    }

    override fun findById(geoLocationId: GeoLocationId): Optional<AddressModel> {
        return addressJpaRepository.findById(geoLocationId.id)
            .map { addressJpaMapper.addressToDomainModel(it) }
    }

    override fun findAll(): List<AddressModel> {
        return addressJpaRepository.findAll()
            .map { addressJpaMapper.addressToDomainModel(it) }
    }

    override fun deleteById(geoLocationId: GeoLocationId) {
        addressJpaRepository.deleteById(geoLocationId.id)
    }

    override fun findByDistrictIdAndStreetNameStartingWith(districtId: GeoLocationId, streetNamePrefix: String): List<AddressModel> {
        return addressJpaRepository.findByDistrictIdAndStreetNameStartingWithIgnoreCase(districtId.id, streetNamePrefix)
            .map { addressJpaMapper.addressToDomainModel(it) }
    }
}