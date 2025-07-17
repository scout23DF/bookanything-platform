package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import de.org.dexterity.bookanything.dom01geolocation.application.services.GeoLocationCRUDService
import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.CreateGeoLocationRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.GeoLocationResponse
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.IDeepGeoLocationResponse
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.dtos.UpdateGeoLocationRequest
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers.DeepGeoLocationRestMapper
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.mappers.GeoLocationRestMapper
import de.org.dexterity.bookanything.shared.mediators.HandlersMediatorManager
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/geolocations")
class GeoLocationController(
    private val geoLocationCRUDService: GeoLocationCRUDService,
    private val handlerMediatorManager: HandlersMediatorManager,
    private val geoLocationRestMapper: GeoLocationRestMapper,
    private val deepGeoLocationRestMapper: DeepGeoLocationRestMapper
) {

    @PostMapping("/{type}")
    fun create(
        @PathVariable type: String,
        @RequestBody request: CreateGeoLocationRequest
    ): ResponseEntity<GeoLocationResponse> {

        val geoLocationType = GeoLocationType.valueOf(type.uppercase())
        val newCreatedModel = geoLocationCRUDService.create(geoLocationType, request)

        return ResponseEntity.ok(geoLocationRestMapper.fromIGeoLocationModelToResponse(newCreatedModel))
    }

    @GetMapping("/{type}/{id}")
    fun findById(
        @PathVariable type: String,
        @PathVariable id: Long
    ): ResponseEntity<GeoLocationResponse> {

        val geoLocationType = GeoLocationType.valueOf(type.uppercase())

        return geoLocationCRUDService.findById(geoLocationType, id)
            .map { ResponseEntity.ok(geoLocationRestMapper.fromIGeoLocationModelToResponse(it) ) }
            .orElse(ResponseEntity.notFound().build())
    }

    @GetMapping("/{type}")
    fun findAll(@PathVariable type: String): List<GeoLocationResponse> {

        val geoLocationType = GeoLocationType.valueOf(type.uppercase())

        return geoLocationCRUDService.findAll(geoLocationType).map { geoLocationRestMapper.fromIGeoLocationModelToResponse(it) }
    }

    @GetMapping("/{type}/search-by-name")
    fun searchByParentIdAndNameStartingWith(
        @PathVariable type: String,
        @RequestParam(required = false) parentId: Long?,
        @RequestParam namePrefix: String
    ): List<GeoLocationResponse> {

        val geoLocationType = GeoLocationType.valueOf(type.uppercase())

        return geoLocationCRUDService.searchByParentIdAndNameStartingWith(geoLocationType, parentId, namePrefix)
                                     .map { geoLocationRestMapper.fromIGeoLocationModelToResponse(it) }

    }

    @GetMapping("/{type}/search-by-alias")
    fun searchByParentIdAndAliasStartingWith(
        @PathVariable type: String,
        @RequestParam(required = true) parentId: Long,
        @RequestParam aliasPrefix: String
    ): List<GeoLocationResponse> {

        val geoLocationType = GeoLocationType.valueOf(type.uppercase())

        return geoLocationCRUDService.searchByParentIdAndAliasStartingWith(geoLocationType, parentId, aliasPrefix)
            .map { geoLocationRestMapper.fromIGeoLocationModelToResponse(it) }

    }

    @GetMapping("/{type}/deep-search")
    fun searchDeep(
        @PathVariable type: String,
        @RequestParam(required = false) id: Long?,
        @RequestParam(required = false) name: String?
    ): ResponseEntity<IDeepGeoLocationResponse> {

        if (id == null && name == null) {
            throw IllegalArgumentException("Either ID or name must be provided for deep search.")
        }

        val geoLocationType = GeoLocationType.valueOf(type.uppercase())

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

        val geoLocationType = GeoLocationType.valueOf(type.uppercase())

        val updated = geoLocationCRUDService.update(geoLocationType, id, request)

        return updated?.let { ResponseEntity.ok(geoLocationRestMapper.fromIGeoLocationModelToResponse(it)) } ?: ResponseEntity.notFound().build()
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

    @DeleteMapping("/{type}/all")
    fun deleteAll(@PathVariable type: String): ResponseEntity<Void> {

        val geoLocationType = GeoLocationType.valueOf(type.uppercase())

        geoLocationCRUDService.deleteAll(geoLocationType)

        return ResponseEntity.noContent().build()
    }

}