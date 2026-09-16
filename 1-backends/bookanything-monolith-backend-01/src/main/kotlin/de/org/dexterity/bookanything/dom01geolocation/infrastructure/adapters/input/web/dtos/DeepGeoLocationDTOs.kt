package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import java.util.UUID

// Base interface for deep GeoLocation responses
@JsonIgnoreProperties(ignoreUnknown = true)
sealed interface IDeepGeoLocationResponse {
    val id: Long
    val name: String
    val alias: String?
    val type: GeoLocationType
    val boundaryRepresentation: String?
    val parentId: Long?
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class DeepContinentResponse(
    override val id: Long,
    override val name: String,
    override val alias: String?,
    override val type: GeoLocationType,
    override val boundaryRepresentation: String?,
    override val parentId: Long? = null,
    val regionsList: List<DeepRegionResponse>? = null
) : IDeepGeoLocationResponse

@JsonIgnoreProperties(ignoreUnknown = true)
data class DeepRegionResponse(
    override val id: Long,
    override val name: String,
    override val alias: String?,
    override val type: GeoLocationType,
    override val boundaryRepresentation: String?,
    override val parentId: Long? = null,
    val countriesList: List<DeepCountryResponse>? = null
) : IDeepGeoLocationResponse

@JsonIgnoreProperties(ignoreUnknown = true)
data class DeepCountryResponse(
    override val id: Long,
    override val name: String,
    override val alias: String?,
    override val type: GeoLocationType,
    override val boundaryRepresentation: String?,
    override val parentId: Long? = null,
    val provincesList: List<DeepProvinceResponse>? = null
) : IDeepGeoLocationResponse

@JsonIgnoreProperties(ignoreUnknown = true)
data class DeepProvinceResponse(
    override val id: Long,
    override val name: String,
    override val alias: String?,
    override val type: GeoLocationType,
    override val boundaryRepresentation: String?,
    override val parentId: Long? = null,
    val citiesList: List<DeepCityResponse>? = null
) : IDeepGeoLocationResponse

@JsonIgnoreProperties(ignoreUnknown = true)
data class DeepCityResponse(
    override val id: Long,
    override val name: String,
    override val alias: String?,
    override val type: GeoLocationType,
    override val boundaryRepresentation: String?,
    override val parentId: Long? = null,
    @JsonProperty("isCountryCapital")
    @JsonAlias("countryCapital")
    val isCountryCapital: Boolean? = false,
    @JsonProperty("isProvinceCapital")
    @JsonAlias("provinceCapital")
    val isProvinceCapital: Boolean? = false,
    val districtsList: List<DeepDistrictResponse>? = null
) : IDeepGeoLocationResponse

@JsonIgnoreProperties(ignoreUnknown = true)
data class DeepDistrictResponse(
    override val id: Long,
    override val name: String,
    override val alias: String?,
    override val type: GeoLocationType,
    override val boundaryRepresentation: String?,
    override val parentId: Long? = null,
    val addressesList: List<DeepAddressResponse>? = null
) : IDeepGeoLocationResponse

@JsonIgnoreProperties(ignoreUnknown = true)
data class DeepAddressResponse(
    val id: Long,
    val streetName: String,
    val houseNumber: String?,
    val floorNumber: String?,
    val doorNumber: String?,
    val addressLine2: String?,
    val postalCode: String,
    val districtId: Long,
    val districtName: String,
    val cityName: String,
    val provinceName: String,
    val countryName: String,
    val coordinates: String?
)