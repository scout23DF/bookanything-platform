package de.org.dexterity.bookanything.dom01geolocation.domain.ports

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import java.util.*

sealed interface IGeoLocationBaseCRUDRepositoryPort<T> {

    fun saveNew(targetModel: T): T
    fun update(targetModel: T): T?
    fun existsGeoLocationById(geoLocationId: GeoLocationId): Boolean
    fun findById(geoLocationId: GeoLocationId): Optional<T>
    fun findAll(): List<T>
    fun deleteById(geoLocationId: GeoLocationId)

}

interface IContinentRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<ContinentModel> {
    fun findByNameStartingWith(namePrefix: String): List<ContinentModel>
}

interface IRegionRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<RegionModel> {
    fun findByContinentIdAndNameStartingWith(continentId: GeoLocationId, namePrefix: String): List<RegionModel>
}

interface ICountryRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<CountryModel> {
    fun findByRegionIdAndNameStartingWith(regionId: GeoLocationId, namePrefix: String): List<CountryModel>
}

interface IProvinceRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<ProvinceModel> {
    fun findByCountryIdAndNameStartingWith(countryId: GeoLocationId, namePrefix: String): List<ProvinceModel>
}

interface ICityRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<CityModel> {
    fun findByProvinceIdAndNameStartingWith(provinceId: GeoLocationId, namePrefix: String): List<CityModel>
}

interface IDistrictRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<DistrictModel> {
    fun findByCityIdAndNameStartingWith(cityId: GeoLocationId, namePrefix: String): List<DistrictModel>
}

interface IAddressRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<AddressModel> {
    fun findByDistrictIdAndStreetNameStartingWith(districtId: GeoLocationId, streetNamePrefix: String): List<AddressModel>
}