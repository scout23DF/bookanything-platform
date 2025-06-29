package br.com.geminiproject.dcl.adapter.output.persistence.elasticsearch

import org.locationtech.jts.geom.Point

interface CentroDistribuicaoElasticRepositoryCustom {

    fun findByLocalizacaoWithin(localizacao: Point, raioEmKm: Double): List<CentroDistribuicaoElasticEntity>

}
