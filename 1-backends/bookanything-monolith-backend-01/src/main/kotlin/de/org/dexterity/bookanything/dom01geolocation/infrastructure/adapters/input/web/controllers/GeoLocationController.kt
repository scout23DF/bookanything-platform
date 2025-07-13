package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import de.org.dexterity.bookanything.dom01geolocation.application.usecases.GeoLocationCRUDService
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateGeoLocationRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.GeoLocationResponse
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.UpdateGeoLocationRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.toResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/geolocation")
class GeoLocationController(
    private val geoLocationCRUDService: GeoLocationCRUDService
) {

    @PostMapping("/{type}")
    fun create(
        @PathVariable type: String,
        @RequestBody request: CreateGeoLocationRequest
    ): ResponseEntity<GeoLocationResponse> {
        val geoLocationType = GeoLocationType.valueOf(type.uppercase())
        val created = geoLocationCRUDService.create(geoLocationType, request)
        return ResponseEntity.ok(created.toResponse())
    }

    @GetMapping("/{type}/{id}")
    fun findById(
        @PathVariable type: String,
        @PathVariable id: Long
    ): ResponseEntity<GeoLocationResponse> {
        val geoLocationType = GeoLocationType.valueOf(type.uppercase())
        return geoLocationCRUDService.findById(geoLocationType, id)
            .map { ResponseEntity.ok(it.toResponse()) }
            .orElse(ResponseEntity.notFound().build())
    }

    @GetMapping("/{type}")
    fun findAll(@PathVariable type: String): List<GeoLocationResponse> {
        val geoLocationType = GeoLocationType.valueOf(type.uppercase())
        return geoLocationCRUDService.findAll(geoLocationType).map { it.toResponse() }
    }

    @GetMapping("/{type}/search")
    fun search(
        @PathVariable type: String,
        @RequestParam(required = false) parentId: Long?,
        @RequestParam namePrefix: String
    ): List<GeoLocationResponse> {
        val geoLocationType = GeoLocationType.valueOf(type.uppercase())
        return geoLocationCRUDService.search(geoLocationType, parentId, namePrefix).map { it.toResponse() }
    }

    @PutMapping("/{type}/{id}")
    fun update(
        @PathVariable type: String,
        @PathVariable id: Long,
        @RequestBody request: UpdateGeoLocationRequest
    ): ResponseEntity<GeoLocationResponse> {
        val geoLocationType = GeoLocationType.valueOf(type.uppercase())
        val updated = geoLocationCRUDService.update(geoLocationType, id, request)
        return updated?.let { ResponseEntity.ok(it.toResponse()) } ?: ResponseEntity.notFound().build()
    }

    @DeleteMapping("/{type}/{id}")
    fun deleteById(
        @PathVariable type: String,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        val geoLocationType = GeoLocationType.valueOf(type.uppercase())
        geoLocationCRUDService.deleteById(geoLocationType, id)
        return ResponseEntity.noContent().build()
    }
}