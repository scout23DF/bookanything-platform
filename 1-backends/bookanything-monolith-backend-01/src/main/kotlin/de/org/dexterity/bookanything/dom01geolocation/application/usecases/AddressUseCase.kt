package de.org.dexterity.bookanything.dom01geolocation.application.usecases

import de.org.dexterity.bookanything.dom01geolocation.domain.models.AddressModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.AddressPersistRepositoryPort
import org.springframework.stereotype.Service
import java.util.*

@Service
class AddressUseCase(private val repository: AddressPersistRepositoryPort) {

    fun create(model: AddressModel): AddressModel {
        return repository.saveNew(model)
    }

    fun findById(id: GeoLocationId): Optional<AddressModel> {
        return repository.findById(id)
    }

    fun findAll(): List<AddressModel> {
        return repository.findAll()
    }

    fun update(model: AddressModel): AddressModel? {
        return repository.update(model)
    }

    fun deleteById(id: GeoLocationId) {
        repository.deleteById(id)
    }

    fun findByDistrictIdAndStreetNameStartingWith(districtId: GeoLocationId, streetNamePrefix: String): List<AddressModel> {
        return repository.findByDistrictIdAndStreetNameStartingWith(districtId, streetNamePrefix)
    }

}