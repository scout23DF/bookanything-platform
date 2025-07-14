package de.org.dexterity.bookanything.dom01geolocation.domain.ports

import de.org.dexterity.bookanything.dom01geolocation.domain.models.AddressModel
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import java.util.*

interface AddressPersistRepositoryPort {

    fun saveNew(targetModel: AddressModel): AddressModel
    fun update(targetModel: AddressModel): AddressModel?
    fun existsAddressById(geoLocationId: GeoLocationId): Boolean
    fun findById(geoLocationId: GeoLocationId): Optional<AddressModel>
    fun findAll(): List<AddressModel>
    fun deleteById(geoLocationId: GeoLocationId)

    fun findByDistrictIdAndStreetNameStartingWith(districtId: GeoLocationId, streetNamePrefix: String): List<AddressModel>

}