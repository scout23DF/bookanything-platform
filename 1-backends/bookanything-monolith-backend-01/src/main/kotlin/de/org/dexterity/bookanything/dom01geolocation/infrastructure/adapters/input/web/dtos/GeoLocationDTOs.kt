package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*

// DTOs
data class GeoLocationDTO(val id: Long, val name: String)
data class AddressDTO(
    val id: Long,
    val streetName: String,
    val houseNumber: String?,
    val postalCode: String,
    val districtId: Long
)

// Mappers
fun ContinentModel.toDto() = GeoLocationDTO(id.id, name)
fun RegionModel.toDto() = GeoLocationDTO(id.id, name)
fun CountryModel.toDto() = GeoLocationDTO(id.id, name)
fun ProvinceModel.toDto() = GeoLocationDTO(id.id, name)
fun CityModel.toDto() = GeoLocationDTO(id.id, name)
fun DistrictModel.toDto() = GeoLocationDTO(id.id, name)
fun AddressModel.toDto() = AddressDTO(id.id, streetName, houseNumber, postalCode, district.id.id)

fun GeoLocationDTO.toContinentModel() = ContinentModel(GeoLocationId(id), name)
fun GeoLocationDTO.toRegionModel(continent: ContinentModel) = RegionModel(GeoLocationId(id), name, continent = continent)
fun GeoLocationDTO.toCountryModel(region: RegionModel) = CountryModel(GeoLocationId(id), name, region = region)
fun GeoLocationDTO.toProvinceModel(country: CountryModel) = ProvinceModel(GeoLocationId(id), name, country = country)
fun GeoLocationDTO.toCityModel(province: ProvinceModel) = CityModel(GeoLocationId(id), name, province = province)
fun GeoLocationDTO.toDistrictModel(city: CityModel) = DistrictModel(GeoLocationId(id), name, city = city)
fun AddressDTO.toAddressModel(district: DistrictModel) = AddressModel(GeoLocationId(id), streetName, houseNumber, postalCode = postalCode, district = district, districtName = district.name, cityName = district.city.name, provinceName = district.city.province.name, countryName = district.city.province.country.name)
