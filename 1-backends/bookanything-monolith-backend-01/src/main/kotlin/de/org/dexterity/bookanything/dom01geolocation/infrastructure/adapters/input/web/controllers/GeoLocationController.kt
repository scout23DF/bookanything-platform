package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import de.org.dexterity.bookanything.dom01geolocation.application.usecases.*
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationId
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.*
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
    fun createContinent(@RequestBody dto: GeoLocationDTO): GeoLocationDTO = continentUseCase.create(dto.toContinentModel()).toDto()

    @GetMapping("/continents/{id}")
    fun getContinent(@PathVariable id: Long): ResponseEntity<GeoLocationDTO> =
        continentUseCase.findById(GeoLocationId(id)).map { ResponseEntity.ok(it.toDto()) }.orElse(ResponseEntity.notFound().build())

    // Region Endpoints
    @PostMapping("/regions")
    fun createRegion(@RequestBody dto: GeoLocationDTO): ResponseEntity<GeoLocationDTO> {
        val continent = continentUseCase.findById(GeoLocationId(dto.id) /*TODO: Fix parent id*/).orElse(null) ?: return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(regionUseCase.create(dto.toRegionModel(continent)).toDto())
    }

    // ... similar endpoints for other GeoLocation types and Address

}
