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
    val friendlyId: String
    val name: String
    val alias: String?
    val propertiesDetailsMap: Map<String, Any>?
    val type: GeoLocationType
    val boundaryRepresentation: Geometry?
    val parentId: Long?

    fun humanReadableName(): String
}

data class ContinentModel (
    override val id: GeoLocationId,
    override val friendlyId: String,
    override val name: String,
    override val alias: String? = null,
    override val propertiesDetailsMap: Map<String, Any>? = null,
    override val boundaryRepresentation: Geometry? = null,
    override val parentId: Long? = null,
    val regionsList: List<RegionModel>? = null
) : IGeoLocationModel {
    override val type = GeoLocationType.CONTINENT
    override fun humanReadableName(): String = "Continent: $name"
}

data class RegionModel (
    override val id: GeoLocationId,
    override val friendlyId: String,
    override val name: String,
    override val alias: String? = null,
    override val propertiesDetailsMap: Map<String, Any>? = null,
    override val boundaryRepresentation: Geometry? = null,
    override val parentId: Long?,
    val continent: ContinentModel,
    val countriesList: List<CountryModel>? = null
) : IGeoLocationModel {
    override val type = GeoLocationType.REGION
    override fun humanReadableName(): String = "Region: $name, ${continent.humanReadableName()}"
}

data class CountryModel (
    override val id: GeoLocationId,
    override val friendlyId: String,
    override val name: String,
    override val alias: String? = null,
    override val propertiesDetailsMap: Map<String, Any>? = null,
    override val boundaryRepresentation: Geometry? = null,
    override val parentId: Long?,
    val region: RegionModel,
    val provincesList: List<ProvinceModel>? = null
) : IGeoLocationModel {
    override val type = GeoLocationType.COUNTRY
    override fun humanReadableName(): String = "Country: $name - ($alias), ${region.humanReadableName()}"
}

data class ProvinceModel (
    override val id: GeoLocationId,
    override val friendlyId: String,
    override val name: String,
    override val alias: String? = null,
    override val propertiesDetailsMap: Map<String, Any>? = null,
    override val boundaryRepresentation: Geometry? = null,
    override val parentId: Long?,
    val country: CountryModel,
    val citiesList: List<CityModel>? = null
) : IGeoLocationModel {
    override val type = GeoLocationType.PROVINCE
    override fun humanReadableName(): String = "Province: $name - ($alias), ${country.humanReadableName()}"
}

data class CityModel (
    override val id: GeoLocationId,
    override val friendlyId: String,
    override val name: String,
    override val alias: String? = null,
    override val propertiesDetailsMap: Map<String, Any>? = null,
    override val boundaryRepresentation: Geometry? = null,
    val isCountryCapital: Boolean? = false,
    val isProvinceCapital: Boolean? = false,
    override val parentId: Long?,
    val province: ProvinceModel,
    val districtsList: List<DistrictModel>? = null
) : IGeoLocationModel {
    override val type = GeoLocationType.CITY
    override fun humanReadableName(): String = "City: $name, ${province.humanReadableName()}"
}

data class DistrictModel (
    override val id: GeoLocationId,
    override val friendlyId: String,
    override val name: String,
    override val alias: String? = null,
    override val propertiesDetailsMap: Map<String, Any>? = null,
    override val boundaryRepresentation: Geometry? = null,
    override val parentId: Long?,
    val city: CityModel,
    val addressesList: List<AddressModel>? = null
) : IGeoLocationModel {
    override val type = GeoLocationType.DISTRICT
    override fun humanReadableName(): String = "District: $name, ${city.humanReadableName()}"
}
