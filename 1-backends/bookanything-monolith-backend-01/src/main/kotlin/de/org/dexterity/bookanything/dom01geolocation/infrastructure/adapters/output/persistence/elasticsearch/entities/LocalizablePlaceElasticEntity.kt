package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.entities

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.GeoPointField
import org.springframework.data.elasticsearch.core.geo.GeoPoint
import java.util.UUID

@Document(indexName = "doc_localizable_places")
data class LocalizablePlaceElasticEntity(
    @Id
    val id: UUID,
    val friendlyId: String,
    val name: String,
    val alias: String?,
    val additionalDetailsMap: Map<String, Any>?,
    @GeoPointField
    val locationPoint: GeoPoint,
    val locationAsGeoHash: String? = null
)