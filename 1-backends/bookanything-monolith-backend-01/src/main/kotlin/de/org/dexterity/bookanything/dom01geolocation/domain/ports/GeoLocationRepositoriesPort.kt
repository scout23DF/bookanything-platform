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

interface IContinentRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<ContinentModel>

interface IRegionRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<RegionModel>

interface ICountryRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<CountryModel>

interface IProvinceRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<ProvinceModel>

interface ICityRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<CityModel>

interface IDistrictRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<DistrictModel>

interface IAddressRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<AddressModel>
