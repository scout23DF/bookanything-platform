package de.org.dexterity.bookanything.dom01geolocation.domain.ports

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import org.locationtech.jts.geom.Geometry
import java.util.*

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

sealed interface IGeoLocationBaseCRUDRepositoryPort<T : Any> {

    fun saveNew(targetModel: T): T
    fun update(targetModel: T): T?
    fun updateBoundary(id: GeoLocationId, boundary: Geometry): T?
    fun existsGeoLocationById(geoLocationId: GeoLocationId): Boolean
    fun findById(geoLocationId: GeoLocationId): Optional<T>
    fun findAll(pageable: Pageable): Page<T>
    fun findDeepById(geoLocationId: GeoLocationId): Optional<T>
    fun findDeepByName(name: String): Optional<T>
    fun deleteById(geoLocationId: GeoLocationId)
    fun deleteAll()

}

interface IContinentRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<ContinentModel> {
    fun findByNameStartingWith(namePrefix: String, pageable: Pageable): Page<ContinentModel>
    fun findByAliasStartingWith(searchedAlias: String, pageable: Pageable): Page<ContinentModel>
    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String, pageable: Pageable): Page<ContinentModel>
    fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<ContinentModel>
}

interface IRegionRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<RegionModel> {
    fun findAllByContinentId(continentId: GeoLocationId, pageable: Pageable): Page<RegionModel>
    fun findByContinentIdAndNameStartingWith(continentId: GeoLocationId, namePrefix: String, pageable: Pageable): Page<RegionModel>
    fun findByContinentIdAndAliasStartingWith(continentId: GeoLocationId, searchedAlias: String, pageable: Pageable): Page<RegionModel>
    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String, pageable: Pageable): Page<RegionModel>
    fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<RegionModel>
}

interface ICountryRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<CountryModel> {
    fun findAllByRegionId(regionId: GeoLocationId, pageable: Pageable): Page<CountryModel>
    fun findByRegionIdAndNameStartingWith(regionId: GeoLocationId, namePrefix: String, pageable: Pageable): Page<CountryModel>
    fun findByRegionIdAndAliasStartingWith(regionId: GeoLocationId, searchedAlias: String, pageable: Pageable): Page<CountryModel>
    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String, pageable: Pageable): Page<CountryModel>
    fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<CountryModel>
}

interface IProvinceRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<ProvinceModel> {
    fun findAllByCountryId(countryId: GeoLocationId, pageable: Pageable): Page<ProvinceModel>
    fun findByCountryIdAndNameStartingWith(countryId: GeoLocationId, namePrefix: String, pageable: Pageable): Page<ProvinceModel>
    fun findByCountryIdAndAliasStartingWith(countryId: GeoLocationId, searchedAlias: String, pageable: Pageable): Page<ProvinceModel>
    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String, pageable: Pageable): Page<ProvinceModel>
    fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<ProvinceModel>
}

interface ICityRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<CityModel> {
    fun findAllByProvinceId(provinceId: GeoLocationId, pageable: Pageable): Page<CityModel>
    fun findByProvinceIdAndNameStartingWith(provinceId: GeoLocationId, namePrefix: String, pageable: Pageable): Page<CityModel>
    fun findByProvinceIdAndAliasStartingWith(provinceId: GeoLocationId, searchedAlias: String, pageable: Pageable): Page<CityModel>
    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String, pageable: Pageable): Page<CityModel>
    fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<CityModel>
}

interface IDistrictRepositoryPort: IGeoLocationBaseCRUDRepositoryPort<DistrictModel> {
    fun findAllByCityId(cityId: GeoLocationId, pageable: Pageable): Page<DistrictModel>
    fun findByCityIdAndNameStartingWith(cityId: GeoLocationId, namePrefix: String, pageable: Pageable): Page<DistrictModel>
    fun findByCityIdAndAliasStartingWith(cityId: GeoLocationId, searchedAlias: String, pageable: Pageable): Page<DistrictModel>
    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String, pageable: Pageable): Page<DistrictModel>
    fun findByPropertiesDetailsMapContains(key: String, value: String, pageable: Pageable): Page<DistrictModel>
}
