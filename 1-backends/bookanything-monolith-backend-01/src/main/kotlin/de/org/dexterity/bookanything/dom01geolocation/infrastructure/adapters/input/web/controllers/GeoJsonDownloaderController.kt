package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.input.web.controllers

import de.org.dexterity.bookanything.dom01geolocation.application.usecases.GeoJsonDownloaderUseCase
import de.org.dexterity.bookanything.dom01geolocation.application.usecases.GeoJsonImporterUseCase
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoJsonDownloadRequest
import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoJsonDownloadResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/geolocation/geojson/downloads")
class GeoJsonDownloaderController(
    private val geoJsonDownloaderUseCase: GeoJsonDownloaderUseCase,
    private val geoJsonImporterUseCase: GeoJsonImporterUseCase
) {

    @PostMapping
    fun downloadGeoJsonFiles(@RequestBody request: GeoJsonDownloadRequest): ResponseEntity<GeoJsonDownloadResponse> {
        val job = geoJsonDownloaderUseCase.initiateDownload(request)
        return ResponseEntity.accepted().body(
            GeoJsonDownloadResponse(
                jobId = job.jobId,
                message = "GeoJSON download job accepted. It will be processed asynchronously."
            )
        )
    }

}