package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import java.util.UUID

// Base interface for deep GeoLocation responses
sealed interface IDeepGeoLocationResponse {
    val id: Long
    val name: String
    val alias: String?
    val type: GeoLocationType
    val boundaryRepresentation: String?
}

data class DeepContinentResponse(
    override val id: Long,
    override val name: String,
    override val alias: String?,
    override val type: GeoLocationType,
    override val boundaryRepresentation: String?,
    val regionsList: List<DeepRegionResponse>? = null
) : IDeepGeoLocationResponse

data class DeepRegionResponse(
    override val id: Long,
    override val name: String,
    override val alias: String?,
    override val type: GeoLocationType,
    override val boundaryRepresentation: String?,
    val countriesList: List<DeepCountryResponse>? = null
) : IDeepGeoLocationResponse

data class DeepCountryResponse(
    override val id: Long,
    override val name: String,
    override val alias: String?,
    override val type: GeoLocationType,
    override val boundaryRepresentation: String?,
    val provincesList: List<DeepProvinceResponse>? = null
) : IDeepGeoLocationResponse

data class DeepProvinceResponse(
    override val id: Long,
    override val name: String,
    override val alias: String?,
    override val type: GeoLocationType,
    override val boundaryRepresentation: String?,
    val citiesList: List<DeepCityResponse>? = null
) : IDeepGeoLocationResponse

data class DeepCityResponse(
    override val id: Long,
    override val name: String,
    override val alias: String?,
    override val type: GeoLocationType,
    override val boundaryRepresentation: String?,
    val isCountryCapital: Boolean? = false,
    val isProvinceCapital: Boolean? = false,
    val districtsList: List<DeepDistrictResponse>? = null
) : IDeepGeoLocationResponse

data class DeepDistrictResponse(
    override val id: Long,
    override val name: String,
    override val alias: String?,
    override val type: GeoLocationType,
    override val boundaryRepresentation: String?,
    val addressesList: List<DeepAddressResponse>? = null
) : IDeepGeoLocationResponse

data class DeepAddressResponse(
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
    val countryName: String,
    val coordinates: String?
)