package de.org.dexterity.bookanything.dom01geolocation.domain.ports

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import org.locationtech.jts.geom.Geometry
import java.util.*

sealed interface IGeoLocationBaseCRUDRepositoryPort<T> {

    fun saveNew(targetModel: T): T
    fun update(targetModel: T): T?
    fun updateBoundary(id: GeoLocationId, boundary: Geometry): T?
    fun existsGeoLocationById(geoLocationId: GeoLocationId): Boolean
    fun findById(geoLocationId: GeoLocationId): Optional<T>
    fun findAll(): List<T>
    fun findDeepById(geoLocationId: GeoLocationId): Optional<T>
    fun findDeepByName(name: String): Optional<T>
    fun deleteById(geoLocationId: GeoLocationId)
    fun deleteAll()

}

interface IContinentRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<ContinentModel> {
    fun findByNameStartingWith(namePrefix: String): List<ContinentModel>
    fun findByAliasStartingWith(searchedAlias: String): List<ContinentModel>
    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String): List<ContinentModel>
    fun findByPropertiesDetailsMapContains(key: String, value: String): List<ContinentModel>
}

interface IRegionRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<RegionModel> {
    fun findByContinentIdAndNameStartingWith(continentId: GeoLocationId, namePrefix: String): List<RegionModel>
    fun findByContinentIdAndAliasStartingWith(continentId: GeoLocationId, searchedAlias: String): List<RegionModel>
    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String): List<RegionModel>
    fun findByPropertiesDetailsMapContains(key: String, value: String): List<RegionModel>
}

interface ICountryRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<CountryModel> {
    fun findByRegionIdAndNameStartingWith(regionId: GeoLocationId, namePrefix: String): List<CountryModel>
    fun findByRegionIdAndAliasStartingWith(regionId: GeoLocationId, searchedAlias: String): List<CountryModel>
    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String): List<CountryModel>
    fun findByPropertiesDetailsMapContains(key: String, value: String): List<CountryModel>
}

interface IProvinceRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<ProvinceModel> {
    fun findByCountryIdAndNameStartingWith(countryId: GeoLocationId, namePrefix: String): List<ProvinceModel>
    fun findByCountryIdAndAliasStartingWith(countryId: GeoLocationId, searchedAlias: String): List<ProvinceModel>
    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String): List<ProvinceModel>
    fun findByPropertiesDetailsMapContains(key: String, value: String): List<ProvinceModel>
}

interface ICityRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<CityModel> {
    fun findByProvinceIdAndNameStartingWith(provinceId: GeoLocationId, namePrefix: String): List<CityModel>
    fun findByProvinceIdAndAliasStartingWith(provinceId: GeoLocationId, searchedAlias: String): List<CityModel>
    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String): List<CityModel>
    fun findByPropertiesDetailsMapContains(key: String, value: String): List<CityModel>
}

interface IDistrictRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<DistrictModel> {
    fun findByCityIdAndNameStartingWith(cityId: GeoLocationId, namePrefix: String): List<DistrictModel>
    fun findByCityIdAndAliasStartingWith(cityId: GeoLocationId, searchedAlias: String): List<DistrictModel>
    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String): List<DistrictModel>
    fun findByPropertiesDetailsMapContains(key: String, value: String): List<DistrictModel>
}
