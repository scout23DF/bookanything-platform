package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.repositories

import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.entities.LocalizablePlaceElasticEntity
import org.locationtech.jts.geom.Point
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHits
import org.springframework.data.elasticsearch.core.geo.GeoPoint
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class LocalizablePlaceElasticRepositoryImpl(private val elasticsearchOperations: ElasticsearchOperations) : LocalizablePlaceElasticRepositoryCustom {

    override fun findByLocationPointWithin(locationPoint: Point, raioEmKm: Double): List<LocalizablePlaceElasticEntity> {

        val geoPoint = GeoPoint(locationPoint.y, locationPoint.x)
        val query = CriteriaQuery(Criteria.where("locationPoint").within(geoPoint, "${raioEmKm}km"))
        val searchHits: SearchHits<LocalizablePlaceElasticEntity> = elasticsearchOperations.search(query as Query, LocalizablePlaceElasticEntity::class.java)
        return searchHits.map { searchHit -> searchHit.content }.toList()

    }

    override fun findByAlias(alias: String): List<LocalizablePlaceElasticEntity> {
        val query = CriteriaQuery(Criteria.where("alias").`is`(alias))
        val searchHits: SearchHits<LocalizablePlaceElasticEntity> = elasticsearchOperations.search(query as Query, LocalizablePlaceElasticEntity::class.java)
        return searchHits.map { searchHit -> searchHit.content }.toList()
    }

}
