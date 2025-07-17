package de.org.dexterity.bookanything.dom01geolocation.domain.models

enum class StatusType {
    ACTIVE, INACTIVE, INVALID
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