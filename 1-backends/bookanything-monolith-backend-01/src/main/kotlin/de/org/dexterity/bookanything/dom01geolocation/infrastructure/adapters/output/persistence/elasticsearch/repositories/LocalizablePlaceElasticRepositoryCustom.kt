package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.repositories

import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.entities.LocalizablePlaceElasticEntity
import org.locationtech.jts.geom.Point

interface LocalizablePlaceElasticRepositoryCustom {

    fun findByLocationPointWithin(locationPoint: Point, raioEmKm: Double): List<LocalizablePlaceElasticEntity>

}
