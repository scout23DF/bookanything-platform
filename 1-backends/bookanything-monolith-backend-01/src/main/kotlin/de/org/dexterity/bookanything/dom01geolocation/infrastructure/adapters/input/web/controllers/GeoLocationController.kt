package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationCRUDService
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateGeoLocationRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.GeoLocationResponse
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.IDeepGeoLocationResponse
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.UpdateGeoLocationRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers.DeepGeoLocationRestMapper
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers.GeoLocationRestMapper
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/geolocations")
class GeoLocationController(
    private val geoLocationCRUDService: GeoLocationCRUDService,
    private val geoLocationRestMapper: GeoLocationRestMapper,
    private val deepGeoLocationRestMapper: DeepGeoLocationRestMapper
) {

    @PostMapping("/{type}")
    fun create(
        @PathVariable type: String,
        @RequestBody request: CreateGeoLocationRequest
    ): ResponseEntity<GeoLocationResponse> {
        val geoLocationType = parseGeoLocationType(type)
        val newCreatedModel = geoLocationCRUDService.create(geoLocationType, request)
        return ResponseEntity.ok(geoLocationRestMapper.fromIGeoLocationModelToResponse(newCreatedModel, true))
    }

    @GetMapping("/{type}/{id}")
    fun findById(
        @PathVariable type: String,
        @PathVariable id: Long,
        @RequestParam(name = "includeBoundary", defaultValue = "false") includeBoundary: Boolean
    ): ResponseEntity<GeoLocationResponse> {
        val geoLocationType = parseGeoLocationType(type)
        return geoLocationCRUDService.findById(geoLocationType, id)
            .map { ResponseEntity.ok(geoLocationRestMapper.fromIGeoLocationModelToResponse(it, includeBoundary) ) }
            .orElse(ResponseEntity.notFound().build())
    }

    @GetMapping("/{type}")
    fun findAll(
        @PathVariable type: String,
        @RequestParam(name = "includeBoundary", defaultValue = "false") includeBoundary: Boolean
    ): ResponseEntity<List<GeoLocationResponse>> {
        val geoLocationType = parseGeoLocationType(type)
        val results = geoLocationCRUDService.findAll(geoLocationType)
            .map { geoLocationRestMapper.fromIGeoLocationModelToResponse(it, includeBoundary) }
        return ResponseEntity.ok(results)
    }

    @GetMapping("/{type}/search-by-name")
    fun searchByParentIdAndNameStartingWith(
        @PathVariable type: String,
        @RequestParam(required = false) parentId: Long?,
        @RequestParam namePrefix: String,
        @RequestParam(name = "includeBoundary", defaultValue = "false") includeBoundary: Boolean
    ): ResponseEntity<List<GeoLocationResponse>> {
        val geoLocationType = parseGeoLocationType(type)
        val results = geoLocationCRUDService.searchByParentIdAndNameStartingWith(geoLocationType, parentId, namePrefix)
            .map { geoLocationRestMapper.fromIGeoLocationModelToResponse(it, includeBoundary) }
        return ResponseEntity.ok(results)
    }

    @GetMapping("/{type}/search-by-alias")
    fun searchByParentIdAndAliasStartingWith(
        @PathVariable type: String,
        @RequestParam(required = true) parentId: Long,
        @RequestParam aliasPrefix: String,
        @RequestParam(name = "includeBoundary", defaultValue = "false") includeBoundary: Boolean
    ): ResponseEntity<List<GeoLocationResponse>> {
        val geoLocationType = parseGeoLocationType(type)
        val results = geoLocationCRUDService.searchByParentIdAndAliasStartingWith(geoLocationType, parentId, aliasPrefix)
            .map { geoLocationRestMapper.fromIGeoLocationModelToResponse(it, includeBoundary) }
        return ResponseEntity.ok(results)
    }

    @GetMapping("/{type}/deep-search")
    fun searchDeep(
        @PathVariable type: String,
        @RequestParam(required = false) id: Long?,
        @RequestParam(required = false) name: String?
    ): ResponseEntity<IDeepGeoLocationResponse> {

        require(id != null || name != null) { "Either ID or name must be provided for deep search." }

        val geoLocationType = parseGeoLocationType(type)
        val foundModel = geoLocationCRUDService.findDeepGeoLocation(geoLocationType, id, name)

        return foundModel?.let {
            ResponseEntity.ok(
                deepGeoLocationRestMapper.fromIGeoLocationModelToDeepResponse(it)
            )
        } ?: ResponseEntity.notFound().build()
    }

    @PutMapping("/{type}/{id}")
    fun update(
        @PathVariable type: String,
        @PathVariable id: Long,
        @RequestBody request: UpdateGeoLocationRequest
    ): ResponseEntity<GeoLocationResponse> {
        val geoLocationType = parseGeoLocationType(type)
        val updated = geoLocationCRUDService.update(geoLocationType, id, request)

        return updated?.let { ResponseEntity.ok(geoLocationRestMapper.fromIGeoLocationModelToResponse(it, true)) } ?: ResponseEntity.notFound().build()
    }

    @DeleteMapping("/{type}/{id}")
    fun deleteById(
        @PathVariable type: String,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        val geoLocationType = parseGeoLocationType(type)
        geoLocationCRUDService.deleteById(geoLocationType, id)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{type}/all")
    fun deleteAll(@PathVariable type: String): ResponseEntity<Void> {
        val geoLocationType = parseGeoLocationType(type)
        geoLocationCRUDService.deleteAll(geoLocationType)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{type}/by-parent/{parentId}")
    fun deleteByParentId(
        @PathVariable type: String,
        @PathVariable parentId: Long
    ): ResponseEntity<Void> {
        val geoLocationType = parseGeoLocationType(type)
        geoLocationCRUDService.deleteByParentId(geoLocationType, parentId)
        return ResponseEntity.noContent().build()
    }

    private fun parseGeoLocationType(type: String): GeoLocationType {
        return GeoLocationType.valueOf(type.uppercase())
    }

    @GetMapping("/{type}/search-by-friendlyid")
    fun searchByFriendlyId(
        @PathVariable type: String,
        @RequestParam friendlyId: String,
        @RequestParam(name = "includeBoundary", defaultValue = "false") includeBoundary: Boolean
    ): ResponseEntity<List<GeoLocationResponse>> {
        val geoLocationType = parseGeoLocationType(type)
        val results = geoLocationCRUDService.findByFriendlyId(geoLocationType, friendlyId)
            .map { geoLocationRestMapper.fromIGeoLocationModelToResponse(it, includeBoundary) }
        return ResponseEntity.ok(results)
    }

    @GetMapping("/{type}/search-by-additional-detail")
    fun searchByAdditionalProperty(
        @PathVariable type: String,
        @RequestParam key: String,
        @RequestParam value: String,
        @RequestParam(name = "includeBoundary", defaultValue = "false") includeBoundary: Boolean
    ): ResponseEntity<List<GeoLocationResponse>> {
        val geoLocationType = parseGeoLocationType(type)
        val results = geoLocationCRUDService.findByPropertiesDetailsMap(geoLocationType, key, value)
            .map { geoLocationRestMapper.fromIGeoLocationModelToResponse(it, includeBoundary) }
        return ResponseEntity.ok(results)
    }

}