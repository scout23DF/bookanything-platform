package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers

import de.org.dexterity.bookanything.dom01geolocation.domain.models.*
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.AddressResponse
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateAddressRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.UpdateAddressRequest
import de.org.dexterity.bookanything.shared.annotations.Mapper


@Mapper
class AddressRestMapper {

    fun fromAddressModelToResponse(sourceModel: AddressModel) : AddressResponse {
        return AddressResponse(
            id = sourceModel.id.id,
            streetName = sourceModel.streetName,
            houseNumber = sourceModel.houseNumber,
            floorNumber = sourceModel.floorNumber,
            doorNumber = sourceModel.doorNumber,
            addressLine2 = sourceModel.addressLine2,
            postalCode = sourceModel.postalCode,
            districtName = sourceModel.districtName,
            cityName = sourceModel.cityName,
            provinceName = sourceModel.provinceName,
            countryName = sourceModel.countryName
        )
    }

    fun fromCreateAddressRequestToAddressModel(createAddressRequest: CreateAddressRequest, district: DistrictModel) : AddressModel {

        return AddressModel(
            id = GeoLocationId(0),
            streetName = createAddressRequest.streetName,
            houseNumber = createAddressRequest.houseNumber,
            floorNumber = createAddressRequest.floorNumber,
            doorNumber = createAddressRequest.doorNumber,
            addressLine2 = createAddressRequest.addressLine2,
            postalCode = createAddressRequest.postalCode,
            district = district,
            districtName = district.name,
            cityName = district.city.name,
            provinceName = district.city.province.name,
            countryName = district.city.province.country.name,
            coordinates = GeoCoordinate(createAddressRequest.latitude, createAddressRequest.longitude),
            status = StatusType.valueOf(createAddressRequest.status)
        )

    }

    fun fromUpdateAddressRequestToAddressModel(updateAddressRequest: UpdateAddressRequest, existingAddress: AddressModel) : AddressModel {
        return existingAddress.copy(
            streetName = updateAddressRequest.streetName,
            houseNumber = updateAddressRequest.houseNumber,
            floorNumber = updateAddressRequest.floorNumber,
            doorNumber = updateAddressRequest.doorNumber,
            addressLine2 = updateAddressRequest.addressLine2,
            postalCode = updateAddressRequest.postalCode
        )
    }

}