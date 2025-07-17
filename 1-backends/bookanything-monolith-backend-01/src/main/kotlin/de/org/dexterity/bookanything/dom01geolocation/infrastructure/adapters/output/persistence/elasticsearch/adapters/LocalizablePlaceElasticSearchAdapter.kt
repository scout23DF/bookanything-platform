package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.LocalizablePlaceQueryRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.entities.LocalizablePlaceElasticEntity
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.repositories.LocalizablePlaceElasticRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.springframework.data.elasticsearch.core.geo.GeoPoint
import org.springframework.stereotype.Component
import java.util.*

@Component("centroDistribuicaoElasticSearchAdapter")
class LocalizablePlaceElasticSearchAdapter(
    private val localizablePlaceElasticRepository: LocalizablePlaceElasticRepository
) : LocalizablePlaceQueryRepositoryPort {

    override fun buscarPorId(id: UUID): LocalizablePlaceModel? {

        return localizablePlaceElasticRepository.findById(id)
            .map { elasticEntity ->
                LocalizablePlaceModel(
                    id = elasticEntity.id,
                    name = elasticEntity.name,
                    alias = elasticEntity.alias,
                    locationPoint = GeometryFactory().createPoint(
                        Coordinate(
                            elasticEntity.locationPoint.lon,
                            elasticEntity.locationPoint.lat
                        )
                    )
                )
            }
            .orElse(null)

    }

    override fun buscarTodos(): List<LocalizablePlaceModel> {

        return localizablePlaceElasticRepository.findAll().map { elasticEntity ->
            LocalizablePlaceModel(
                id = elasticEntity.id,
                name = elasticEntity.name,
                alias = elasticEntity.alias,
                locationPoint = GeometryFactory().createPoint(
                    Coordinate(
                        elasticEntity.locationPoint.lon,
                        elasticEntity.locationPoint.lat
                    )
                )
            )
        }.toList()

    }

    override fun buscarPorAliasIniciandoPor(alias: String): List<LocalizablePlaceModel> {
        return localizablePlaceElasticRepository.findByAliasStartingWith(alias).map { elasticEntity ->
            LocalizablePlaceModel(
                id = elasticEntity.id,
                name = elasticEntity.name,
                alias = elasticEntity.alias,
                locationPoint = GeometryFactory().createPoint(
                    Coordinate(
                        elasticEntity.locationPoint.lon,
                        elasticEntity.locationPoint.lat
                    )
                )
            )
        }.toList()
    }

    override fun buscarCentrosProximos(
        locationPoint: Point,
        raioEmKm: Double
    ): List<LocalizablePlaceModel> {

        return localizablePlaceElasticRepository.findByLocationPointWithin(locationPoint, raioEmKm).map { elasticEntity ->
            LocalizablePlaceModel(
                id = elasticEntity.id,
                name = elasticEntity.name,
                alias = elasticEntity.alias,
                locationPoint = GeometryFactory().createPoint(
                    Coordinate(
                        elasticEntity.locationPoint.lon,
                        elasticEntity.locationPoint.lat
                    )
                )
            )
        }.toList()

    }

    override fun synchronizeFromWriteRepository(sourceLocalizablePlacesList : List<LocalizablePlaceModel>?): Map<String, Int> {
        val resultMap: MutableMap<String, Int> = HashMap()

        resultMap["before.records-in-writeable-repository.count"] = sourceLocalizablePlacesList?.size ?: 0
        resultMap["before.entries-in-queryable-repository.count"] = localizablePlaceElasticRepository.count().toInt()

        // 1. Limpar todos os dados existentes no Elasticsearch
        localizablePlaceElasticRepository.deleteAll()

        // 3. Indexar os dados do PostgreSQL no Elasticsearch
        sourceLocalizablePlacesList?.let { localizablePlaceFromDBList ->
            val elasticEntitiesList = localizablePlaceFromDBList.map { oneLocalizablePlaceFromDB ->
                LocalizablePlaceElasticEntity(
                    id = oneLocalizablePlaceFromDB.id,
                    name = oneLocalizablePlaceFromDB.name,
                    alias = oneLocalizablePlaceFromDB.alias,
                    locationPoint = GeoPoint(
                        oneLocalizablePlaceFromDB.locationPoint.y,
                        oneLocalizablePlaceFromDB.locationPoint.x
                    )
                )
            }
            val savedEntriesInReadRepository = this.localizablePlaceElasticRepository.saveAll(elasticEntitiesList)
            resultMap["after.entries-in-queryable-repository.count"] = savedEntriesInReadRepository.count().toInt()

        }

        println("Synchronization done: ${resultMap}")

        return resultMap
    }

    override fun deletarTodos() {
        localizablePlaceElasticRepository.deleteAll()
    }
}