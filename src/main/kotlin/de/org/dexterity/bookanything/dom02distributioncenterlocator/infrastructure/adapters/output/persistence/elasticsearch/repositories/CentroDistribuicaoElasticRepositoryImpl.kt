package de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.output.persistence.elasticsearch

import de.org.dexterity.bookanything.dom02distributioncenterlocator.infrastructure.adapters.output.persistence.elasticsearch.entities.CentroDistribuicaoElasticEntity
import org.locationtech.jts.geom.Point
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHits
import org.springframework.data.elasticsearch.core.geo.GeoPoint
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class CentroDistribuicaoElasticRepositoryImpl(private val elasticsearchOperations: ElasticsearchOperations) : CentroDistribuicaoElasticRepositoryCustom {

    override fun findByLocalizacaoWithin(localizacao: Point, raioEmKm: Double): List<CentroDistribuicaoElasticEntity> {

        val geoPoint = GeoPoint(localizacao.y, localizacao.x)
        val query = CriteriaQuery(Criteria.where("localizacao").within(geoPoint, "${raioEmKm}km"))
        val searchHits: SearchHits<CentroDistribuicaoElasticEntity> = elasticsearchOperations.search(query as Query, CentroDistribuicaoElasticEntity::class.java)
        return searchHits.map { searchHit -> searchHit.content }.toList()

    }

}
