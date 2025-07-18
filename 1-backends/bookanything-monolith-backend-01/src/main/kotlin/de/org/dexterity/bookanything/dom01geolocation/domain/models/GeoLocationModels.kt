package de.org.dexterity.bookanything.dom01geolocation.domain.models

import org.locationtech.jts.geom.Geometry
import kotlin.reflect.KClass

enum class GeoLocationType(val modelClass: KClass<out IGeoLocationModel>) {
    CONTINENT(ContinentModel::class),
    REGION(RegionModel::class),
    COUNTRY(CountryModel::class),
    PROVINCE(ProvinceModel::class),
    CITY(CityModel::class),
    DISTRICT(DistrictModel::class)
}

data class GeoCoordinate(val latitude: Double, val longitude: Double)

data class GeoLocationId(val id: Long)

sealed interface IGeoLocationModel {
    val id: GeoLocationId
    val name: String
    val alias: String?
    val type: GeoLocationType
    val boundaryRepresentation: Geometry?
    val parentId: Long?
}

data class ContinentModel (
    override val id: GeoLocationId,
    override val name: String,
    override val alias: String? = null,
    override val boundaryRepresentation: Geometry? = null,
    override val parentId: Long? = null,
    val regionsList: List<RegionModel>? = null
) : IGeoLocationModel {
    override val type = GeoLocationType.CONTINENT
}

data class RegionModel (
    override val id: GeoLocationId,
    override val name: String,
    override val alias: String? = null,
    override val boundaryRepresentation: Geometry? = null,
    override val parentId: Long?,
    val continent: ContinentModel,
    val countriesList: List<CountryModel>? = null
) : IGeoLocationModel {
    override val type = GeoLocationType.REGION
}

data class CountryModel (
    override val id: GeoLocationId,
    override val name: String,
    override val alias: String? = null,
    override val boundaryRepresentation: Geometry? = null,
    override val parentId: Long?,
    val region: RegionModel,
    val provincesList: List<ProvinceModel>? = null
) : IGeoLocationModel {
    override val type = GeoLocationType.COUNTRY
}

data class ProvinceModel (
    override val id: GeoLocationId,
    override val name: String,
    override val alias: String? = null,
    override val boundaryRepresentation: Geometry? = null,
    override val parentId: Long?,
    val country: CountryModel,
    val citiesList: List<CityModel>? = null
) : IGeoLocationModel {
    override val type = GeoLocationType.PROVINCE
}

data class CityModel (
    override val id: GeoLocationId,
    override val name: String,
    override val alias: String? = null,
    override val boundaryRepresentation: Geometry? = null,
    val isCountryCapital: Boolean? = false,
    val isProvinceCapital: Boolean? = false,
    override val parentId: Long?,
    val province: ProvinceModel,
    val districtsList: List<DistrictModel>? = null
) : IGeoLocationModel {
    override val type = GeoLocationType.CITY
}

data class DistrictModel (
    override val id: GeoLocationId,
    override val name: String,
    override val alias: String? = null,
    override val boundaryRepresentation: Geometry? = null,
    override val parentId: Long?,
    val city: CityModel,
    val addressesList: List<AddressModel>? = null
) : IGeoLocationModel {
    override val type = GeoLocationType.DISTRICT
}
