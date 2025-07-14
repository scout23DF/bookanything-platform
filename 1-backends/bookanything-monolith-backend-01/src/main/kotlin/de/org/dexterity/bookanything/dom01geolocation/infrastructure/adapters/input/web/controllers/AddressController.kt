package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import de.org.dexterity.bookanything.dom01geolocation.application.usecases.AddressUseCase
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.AddressResponse
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateAddressRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.UpdateAddressRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers.AddressRestMapper
import de.org.dexterity.bookanything.dom01geolocation.application.usecases.DistrictUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/addresses")
class AddressController(
    private val addressUseCase: AddressUseCase,
    private val districtUseCase: DistrictUseCase,
    private val addressRestMapper: AddressRestMapper
) {

    @PostMapping
    fun createAddress(@RequestBody dto: CreateAddressRequest): ResponseEntity<AddressResponse> {
        val district = districtUseCase.findById(GeoLocationId(dto.districtId)).orElse(null) ?: return ResponseEntity.badRequest().build()
        val createdAddress = addressUseCase.create(addressRestMapper.fromCreateAddressRequestToAddressModel(dto, district))
        return ResponseEntity.ok(addressRestMapper.fromAddressModelToResponse(createdAddress))
    }

    @GetMapping("/{id}")
    fun getAddress(@PathVariable id: Long): ResponseEntity<AddressResponse> {
        return addressUseCase.findById(GeoLocationId(id))
            .map { addressRestMapper.fromAddressModelToResponse(it) }
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())
    }

    @GetMapping
    fun getAllAddresses(): List<AddressResponse> {
        return addressUseCase.findAll().map { addressRestMapper.fromAddressModelToResponse(it) }
    }

    @PutMapping("/{id}")
    fun updateAddress(@PathVariable id: Long, @RequestBody dto: UpdateAddressRequest): ResponseEntity<AddressResponse> {
        val existingAddress = addressUseCase.findById(GeoLocationId(id)).orElse(null) ?: return ResponseEntity.notFound().build()
        val updatedAddressModel = addressRestMapper.fromUpdateAddressRequestToAddressModel(dto, existingAddress)
        val savedAddress = addressUseCase.update(updatedAddressModel)
        return savedAddress?.let { ResponseEntity.ok(addressRestMapper.fromAddressModelToResponse(it)) } ?: ResponseEntity.notFound().build()
    }

    @DeleteMapping("/{id}")
    fun deleteAddress(@PathVariable id: Long): ResponseEntity<Void> {
        addressUseCase.deleteById(GeoLocationId(id))
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/search")
    fun searchAddresses(
        @RequestParam districtId: Long,
        @RequestParam streetNamePrefix: String
    ): List<AddressResponse> {
        return addressUseCase.findByDistrictIdAndStreetNameStartingWith(GeoLocationId(districtId), streetNamePrefix)
            .map { addressRestMapper.fromAddressModelToResponse(it) }
    }
}