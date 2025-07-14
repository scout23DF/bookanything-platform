package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos

// --- Request DTOs ---
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
