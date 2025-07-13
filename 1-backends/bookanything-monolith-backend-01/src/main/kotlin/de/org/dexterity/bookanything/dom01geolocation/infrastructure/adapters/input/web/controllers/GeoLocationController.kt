package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import de.org.dexterity.bookanything.dom01geolocation.application.usecases.*
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.*
import org.locationtech.jts.io.WKTReader
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/geolocation")
class GeoLocationController(
    private val continentUseCase: ContinentUseCase,
    private val regionUseCase: RegionUseCase,
    private val countryUseCase: CountryUseCase,
    private val provinceUseCase: ProvinceUseCase,
    private val cityUseCase: CityUseCase,
    private val districtUseCase: DistrictUseCase,
    private val addressUseCase: AddressUseCase
) {

    // Continent Endpoints
    @PostMapping("/continents")
    fun createContinent(@RequestBody dto: CreateGeoLocationRequest): GeoLocationResponse {
        return continentUseCase.create(dto.toContinentModel()).toResponse()
    }

    @GetMapping("/continents/{id}")
    fun getContinent(@PathVariable id: Long): ResponseEntity<GeoLocationResponse> {
        return continentUseCase.findById(GeoLocationId(id))
            .map { ResponseEntity.ok(it.toResponse()) }
            .orElse(ResponseEntity.notFound().build())
    }

    @GetMapping("/continents")
    fun getAllContinents(): List<GeoLocationResponse> {
        return continentUseCase.findAll().map { it.toResponse() }
    }

    @PutMapping("/continents/{id}")
    fun updateContinent(@PathVariable id: Long, @RequestBody dto: UpdateGeoLocationRequest): ResponseEntity<GeoLocationResponse> {
        val continent = continentUseCase.findById(GeoLocationId(id)).orElse(null) ?: return ResponseEntity.notFound().build()
        val updatedContinent = continent.copy(name = dto.name, boundaryRepresentation = dto.boundaryRepresentation?.let { WKTReader().read(it) })
        return ResponseEntity.ok(continentUseCase.update(updatedContinent)?.toResponse())
    }

    @DeleteMapping("/continents/{id}")
    fun deleteContinent(@PathVariable id: Long): ResponseEntity<Void> {
        continentUseCase.deleteById(GeoLocationId(id))
        return ResponseEntity.noContent().build()
    }

    // Region Endpoints
    @PostMapping("/regions")
    fun createRegion(@RequestBody dto: CreateGeoLocationRequest): ResponseEntity<GeoLocationResponse> {

        val parent = continentUseCase.findById(GeoLocationId(dto.parentId!!))
                                     .orElse(null) ?: return ResponseEntity.badRequest().build()

        return ResponseEntity.ok(
            regionUseCase.create(dto.toRegionModel(parent))
                                 .toResponse()
        )

    }

    @GetMapping("/regions/{id}")
    fun getRegion(@PathVariable id: Long): ResponseEntity<GeoLocationResponse> {
        return regionUseCase.findById(GeoLocationId(id))
            .map { ResponseEntity.ok(it.toResponse()) }
            .orElse(ResponseEntity.notFound().build())
    }

    @GetMapping("/regions")
    fun getAllRegions(): List<GeoLocationResponse> {
        return regionUseCase.findAll().map { it.toResponse() }
    }

    @PutMapping("/regions/{id}")
    fun updateRegion(@PathVariable id: Long, @RequestBody dto: UpdateGeoLocationRequest): ResponseEntity<GeoLocationResponse> {
        val region = regionUseCase.findById(GeoLocationId(id)).orElse(null) ?: return ResponseEntity.notFound().build()
        val updatedRegion = region.copy(name = dto.name, boundaryRepresentation = dto.boundaryRepresentation?.let { WKTReader().read(it) })
        return ResponseEntity.ok(regionUseCase.update(updatedRegion)?.toResponse())
    }

    @DeleteMapping("/regions/{id}")
    fun deleteRegion(@PathVariable id: Long): ResponseEntity<Void> {
        regionUseCase.deleteById(GeoLocationId(id))
        return ResponseEntity.noContent().build()
    }

    // ... Implement similar endpoints for Country, Province, City, District

    // Address Endpoints
    @PostMapping("/addresses")
    fun createAddress(@RequestBody dto: CreateAddressRequest): ResponseEntity<AddressResponse> {
        val parent = districtUseCase.findById(GeoLocationId(dto.districtId)).orElse(null) ?: return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(addressUseCase.create(dto.toAddressModel(parent)).toResponse())
    }

    @GetMapping("/addresses/{id}")
    fun getAddress(@PathVariable id: Long): ResponseEntity<AddressResponse> =
        addressUseCase.findById(GeoLocationId(id)).map { ResponseEntity.ok(it.toResponse()) }.orElse(ResponseEntity.notFound().build())

    @GetMapping("/addresses")
    fun getAllAddresses(): List<AddressResponse> = addressUseCase.findAll().map { it.toResponse() }

    @PutMapping("/addresses/{id}")
    fun updateAddress(@PathVariable id: Long, @RequestBody dto: UpdateAddressRequest): ResponseEntity<AddressResponse> {
        val address = addressUseCase.findById(GeoLocationId(id)).orElse(null) ?: return ResponseEntity.notFound().build()
        val updatedAddress = address.copy(
            streetName = dto.streetName,
            houseNumber = dto.houseNumber,
            floorNumber = dto.floorNumber,
            doorNumber = dto.doorNumber,
            addressLine2 = dto.addressLine2,
            postalCode = dto.postalCode
        )
        return ResponseEntity.ok(addressUseCase.update(updatedAddress)?.toResponse())
    }

    @DeleteMapping("/addresses/{id}")
    fun deleteAddress(@PathVariable id: Long): ResponseEntity<Void> {
        addressUseCase.deleteById(GeoLocationId(id))
        return ResponseEntity.noContent().build()
    }
}