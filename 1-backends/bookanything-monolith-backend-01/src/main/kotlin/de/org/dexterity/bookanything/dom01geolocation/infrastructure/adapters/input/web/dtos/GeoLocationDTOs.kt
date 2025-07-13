package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTReader

// --- Request DTOs ---
data class CreateGeoLocationRequest(val name: String, val parentId: Long? = null, val boundaryRepresentation: String? = null)
data class UpdateGeoLocationRequest(val name: String, val boundaryRepresentation: String? = null)

data class CreateAddressRequest(
    val streetName: String,
    val houseNumber: String?,
    val floorNumber: String?,
    val doorNumber: String?,
    val addressLine2: String?,
    val postalCode: String,
    val districtId: Long,
    val latitude: Double,
    val longitude: Double,
    val status: String
)

data class UpdateAddressRequest(
    val streetName: String,
    val houseNumber: String?,
    val floorNumber: String?,
    val doorNumber: String?,
    val addressLine2: String?,
    val postalCode: String
)

// --- Response DTOs ---
data class GeoLocationResponse(val id: Long, val name: String, val boundaryRepresentation: String? = null)
data class AddressResponse(
    val id: Long,
    val streetName: String,
    val houseNumber: String?,
    val floorNumber: String?,
    val doorNumber: String?,
    val addressLine2: String?,
    val postalCode: String,
    val districtName: String,
    val cityName: String,
    val provinceName: String,
    val countryName: String
)

// --- Mappers ---
private val wktReader = WKTReader()

private fun String.toGeometry(): Geometry = wktReader.read(this)

fun IGeoLocationModel.toResponse() = GeoLocationResponse(id.id, name, boundaryRepresentation?.toText())

fun CreateGeoLocationRequest.toModel(type: GeoLocationType, parent: IGeoLocationModel? = null): IGeoLocationModel {
    val boundary = boundaryRepresentation?.toGeometry()
    return when (type) {
        GeoLocationType.CONTINENT -> ContinentModel(id = GeoLocationId(0), name = name, boundaryRepresentation = boundary)
        GeoLocationType.REGION -> RegionModel(id = GeoLocationId(0), name = name, boundaryRepresentation = boundary, continent = parent as ContinentModel)
        GeoLocationType.COUNTRY -> CountryModel(id = GeoLocationId(0), name = name, boundaryRepresentation = boundary, region = parent as RegionModel)
        GeoLocationType.PROVINCE -> ProvinceModel(id = GeoLocationId(0), name = name, boundaryRepresentation = boundary, country = parent as CountryModel)
        GeoLocationType.CITY -> CityModel(id = GeoLocationId(0), name = name, boundaryRepresentation = boundary, province = parent as ProvinceModel)
        GeoLocationType.DISTRICT -> DistrictModel(id = GeoLocationId(0), name = name, boundaryRepresentation = boundary, city = parent as CityModel)
    }
}

fun CreateAddressRequest.toAddressModel(district: DistrictModel) = AddressModel(
    id = GeoLocationId(0),
    streetName = streetName,
    houseNumber = houseNumber,
    floorNumber = floorNumber,
    doorNumber = doorNumber,
    addressLine2 = addressLine2,
    postalCode = postalCode,
    district = district,
    districtName = district.name,
    cityName = district.city.name,
    provinceName = district.city.province.name,
    countryName = district.city.province.country.name,
    coordinates = GeoCoordinate(latitude, longitude),
    status = StatusType.valueOf(status)
)