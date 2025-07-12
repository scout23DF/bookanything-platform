package de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.output.persistence.elasticsearch

import de.org.dexterity.bookanything.dom02distributioncenterlocator.infrastructure.adapters.output.persistence.elasticsearch.entities.CentroDistribuicaoElasticEntity
import org.locationtech.jts.geom.Point

interface CentroDistribuicaoElasticRepositoryCustom {

    fun findByLocalizacaoWithin(localizacao: Point, raioEmKm: Double): List<CentroDistribuicaoElasticEntity>

}
