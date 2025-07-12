package de.org.dexterity.bookanything.dom01geolocation.domain.ports

import de.org.dexterity.bookanything.dom01geolocation.domain.dtos.GeoJsonUploadedFileDTO

interface GeoJsonFilePublisherPort {
    fun publish(geoJsonUploadedFileDTO: GeoJsonUploadedFileDTO)
}