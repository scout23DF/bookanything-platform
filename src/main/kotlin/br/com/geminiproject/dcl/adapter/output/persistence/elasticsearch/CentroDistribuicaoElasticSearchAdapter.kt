package br.com.geminiproject.dcl.adapter.output.persistence.elasticsearch

import br.com.geminiproject.dcl.domain.CentroDistribuicaoModel
import br.com.geminiproject.dcl.domain.ports.CentroDistribuicaoQueryRepositoryPort
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.springframework.stereotype.Component
import java.util.*

@Component("centroDistribuicaoElasticSearchAdapter")
class CentroDistribuicaoElasticSearchAdapter(
    private val centroDistribuicaoElasticRepository: CentroDistribuicaoElasticRepository
) : CentroDistribuicaoQueryRepositoryPort {

    override fun buscarPorId(id: UUID): CentroDistribuicaoModel? {

        return centroDistribuicaoElasticRepository.findById(id).get().let { elasticEntity ->
            CentroDistribuicaoModel(
                id = elasticEntity.id,
                nome = elasticEntity.nome,
                localizacao = GeometryFactory().createPoint(Coordinate(elasticEntity.localizacao.lon, elasticEntity.localizacao.lat))
            )
        }

    }

    override fun buscarTodos(): List<CentroDistribuicaoModel> {

        return centroDistribuicaoElasticRepository.findAll().map { elasticEntity ->
            CentroDistribuicaoModel(
                id = elasticEntity.id,
                nome = elasticEntity.nome,
                localizacao = GeometryFactory().createPoint(Coordinate(elasticEntity.localizacao.lon, elasticEntity.localizacao.lat))
            )
        }.toList()

    }

    override fun buscarCentrosProximos(
        localizacao: Point,
        raioEmKm: Double
    ): List<CentroDistribuicaoModel> {

        return centroDistribuicaoElasticRepository.findByLocalizacaoWithin(localizacao, raioEmKm).map { elasticEntity ->
            CentroDistribuicaoModel(
                id = elasticEntity.id,
                nome = elasticEntity.nome,
                localizacao = GeometryFactory().createPoint(Coordinate(elasticEntity.localizacao.lon, elasticEntity.localizacao.lat))
            )
        }.toList()

    }

}