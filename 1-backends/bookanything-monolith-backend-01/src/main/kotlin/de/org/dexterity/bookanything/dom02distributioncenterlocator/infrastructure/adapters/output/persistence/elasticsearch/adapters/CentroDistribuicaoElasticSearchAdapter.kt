package de.org.dexterity.bookanything.dom02distributioncenterlocator.infrastructure.adapters.output.persistence.elasticsearch.adapters

import de.org.dexterity.bookanything.dom02distributioncenterlocator.adapter.output.persistence.elasticsearch.CentroDistribuicaoElasticRepository
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.models.CentroDistribuicaoModel
import de.org.dexterity.bookanything.dom02distributioncenterlocator.domain.ports.CentroDistribuicaoQueryRepositoryPort
import de.org.dexterity.bookanything.dom02distributioncenterlocator.infrastructure.adapters.output.persistence.elasticsearch.entities.CentroDistribuicaoElasticEntity
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.springframework.data.elasticsearch.core.geo.GeoPoint
import org.springframework.stereotype.Component
import java.util.HashMap
import java.util.UUID

@Component("centroDistribuicaoElasticSearchAdapter")
class CentroDistribuicaoElasticSearchAdapter(
    private val centroDistribuicaoElasticRepository: CentroDistribuicaoElasticRepository
) : CentroDistribuicaoQueryRepositoryPort {

    override fun buscarPorId(id: UUID): CentroDistribuicaoModel? {

        return centroDistribuicaoElasticRepository.findById(id)
            .map { elasticEntity ->
                CentroDistribuicaoModel(
                    id = elasticEntity.id,
                    nome = elasticEntity.nome,
                    localizacao = GeometryFactory().createPoint(
                        Coordinate(
                            elasticEntity.localizacao.lon,
                            elasticEntity.localizacao.lat
                        )
                    )
                )
            }
            .orElse(null)

    }

    override fun buscarTodos(): List<CentroDistribuicaoModel> {

        return centroDistribuicaoElasticRepository.findAll().map { elasticEntity ->
            CentroDistribuicaoModel(
                id = elasticEntity.id,
                nome = elasticEntity.nome,
                localizacao = GeometryFactory().createPoint(
                    Coordinate(
                        elasticEntity.localizacao.lon,
                        elasticEntity.localizacao.lat
                    )
                )
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
                localizacao = GeometryFactory().createPoint(
                    Coordinate(
                        elasticEntity.localizacao.lon,
                        elasticEntity.localizacao.lat
                    )
                )
            )
        }.toList()

    }

    override fun synchronizeFromWriteRepository(sourceCentroDistribuicaoList : List<CentroDistribuicaoModel>?): Map<String, Int> {
        val resultMap: MutableMap<String, Int> = HashMap()

        resultMap["before.records-in-writeable-repository.count"] = sourceCentroDistribuicaoList?.size ?: 0
        resultMap["before.entries-in-queryable-repository.count"] = centroDistribuicaoElasticRepository.count().toInt()

        // 1. Limpar todos os dados existentes no Elasticsearch
        centroDistribuicaoElasticRepository.deleteAll()

        // 3. Indexar os dados do PostgreSQL no Elasticsearch
        sourceCentroDistribuicaoList?.let { centrosDistFromDBList ->
            val elasticEntitiesList = centrosDistFromDBList.map { oneCentroDistFromDB ->
                CentroDistribuicaoElasticEntity(
                    id = oneCentroDistFromDB.id,
                    nome = oneCentroDistFromDB.nome,
                    localizacao = GeoPoint(
                        oneCentroDistFromDB.localizacao.y,
                        oneCentroDistFromDB.localizacao.x
                    )
                )
            }
            val savedEntriesInReadRepository = this.centroDistribuicaoElasticRepository.saveAll(elasticEntitiesList)
            resultMap["after.entries-in-queryable-repository.count"] = savedEntriesInReadRepository.count().toInt()

        }

        println("Sincronização concluída: ${resultMap}")

        return resultMap
    }

    override fun deletarTodos() {
        centroDistribuicaoElasticRepository.deleteAll()
    }
}