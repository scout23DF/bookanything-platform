package de.org.dexterity.bookanything.dom01geolocation.domain.models

import org.locationtech.jts.geom.Geometry

enum class StatusType {
    ACTIVE, INACTIVE, INVALID
}

enum class GeoLocationType {
    CONTINENT, REGION, COUNTRY, PROVINCE, CITY, DISTRICT
}

data class GeoCoordinate(val latitude: Double, val longitude: Double)

data class GeoLocationId(val id: Long)

sealed interface IGeoLocationModel {
    val id: GeoLocationId
    val name: String
    val type: GeoLocationType
    val boundaryRepresentation: Geometry? // New field for boundary representation
}

data class ContinentModel (
    override val id: GeoLocationId,
    override val name: String,
    override val boundaryRepresentation: Geometry? = null,
    val regionsList: List<RegionModel>? = null
) : IGeoLocationModel {
    override val type = GeoLocationType.CONTINENT
}

data class RegionModel (
    override val id: GeoLocationId,
    override val name: String,
    override val boundaryRepresentation: Geometry? = null,
    val continent: ContinentModel,
    val countriesList: List<CountryModel>? = null
) : IGeoLocationModel {
    override val type = GeoLocationType.REGION
}

data class CountryModel (
    override val id: GeoLocationId,
    override val name: String,
    override val boundaryRepresentation: Geometry? = null,
    val region: RegionModel,
    val provincesList: List<ProvinceModel>? = null
) : IGeoLocationModel {
    override val type = GeoLocationType.COUNTRY
}

data class ProvinceModel (
    override val id: GeoLocationId,
    override val name: String,
    override val boundaryRepresentation: Geometry? = null,
    val country: CountryModel,
    val citiesList: List<CityModel>? = null
) : IGeoLocationModel {
    override val type = GeoLocationType.PROVINCE
}

data class CityModel (
    override val id: GeoLocationId,
    override val name: String,
    override val boundaryRepresentation: Geometry? = null,
    val isCountryCapital: Boolean? = false,
    val isProvinceCapital: Boolean? = false,
    val province: ProvinceModel,
    val districtsList: List<DistrictModel>? = null
) : IGeoLocationModel {
    override val type = GeoLocationType.CITY
}

data class DistrictModel (
    override val id: GeoLocationId,
    override val name: String,
    override val boundaryRepresentation: Geometry? = null,
    val city: CityModel,
    val addressesList: List<AddressModel>? = null
) : IGeoLocationModel {
    override val type = GeoLocationType.DISTRICT
}

data class AddressModel (
    val id: GeoLocationId,
    val streetName: String,
    val houseNumber: String?,
    val floorNumber: String?,
    val doorNumber: String?,
    val addressLine2: String?,
    val postalCode: String,
    val districtName: String,
    val cityName: String,
    val provinceName: String,
    val countryName: String,
    val coordinates: GeoCoordinate?,
    val status: StatusType? = StatusType.ACTIVE,

    val district: DistrictModel
)
