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

// Model to Response DTO
fun ContinentModel.toResponse() = GeoLocationResponse(id.id, name, boundaryRepresentation?.toText())
fun RegionModel.toResponse() = GeoLocationResponse(id.id, name, boundaryRepresentation?.toText())
fun CountryModel.toResponse() = GeoLocationResponse(id.id, name, boundaryRepresentation?.toText())
fun ProvinceModel.toResponse() = GeoLocationResponse(id.id, name, boundaryRepresentation?.toText())
fun CityModel.toResponse() = GeoLocationResponse(id.id, name, boundaryRepresentation?.toText())
fun DistrictModel.toResponse() = GeoLocationResponse(id.id, name, boundaryRepresentation?.toText())
fun AddressModel.toResponse() = AddressResponse(
    id.id, streetName, houseNumber, floorNumber, doorNumber, addressLine2, postalCode,
    districtName, cityName, provinceName, countryName
)

// Request DTO to Model
fun CreateGeoLocationRequest.toContinentModel() = ContinentModel(id = GeoLocationId(0), name = name, boundaryRepresentation = boundaryRepresentation?.toGeometry())
fun CreateGeoLocationRequest.toRegionModel(continent: ContinentModel) = RegionModel(id = GeoLocationId(0), name = name, continent = continent, boundaryRepresentation = boundaryRepresentation?.toGeometry())
fun CreateGeoLocationRequest.toCountryModel(region: RegionModel) = CountryModel(id = GeoLocationId(0), name = name, region = region, boundaryRepresentation = boundaryRepresentation?.toGeometry())
fun CreateGeoLocationRequest.toProvinceModel(country: CountryModel) = ProvinceModel(id = GeoLocationId(0), name = name, country = country, boundaryRepresentation = boundaryRepresentation?.toGeometry())
fun CreateGeoLocationRequest.toCityModel(province: ProvinceModel) = CityModel(id = GeoLocationId(0), name = name, province = province, boundaryRepresentation = boundaryRepresentation?.toGeometry())
fun CreateGeoLocationRequest.toDistrictModel(city: CityModel) = DistrictModel(id = GeoLocationId(0), name = name, city = city, boundaryRepresentation = boundaryRepresentation?.toGeometry())

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