package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.adapters

import de.org.dexterity.bookanything.dom01geolocation.domain.models.LocalizablePlaceModel
import de.org.dexterity.bookanything.dom01geolocation.domain.ports.LocalizablePlaceQueryRepositoryPort
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.mappers.LocalizablePlaceElasticMapper
import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.repositories.LocalizablePlaceElasticRepository
import org.locationtech.jts.geom.Point
import org.springframework.stereotype.Component
import java.util.*

@Component("centroDistribuicaoElasticSearchAdapter")
class LocalizablePlaceElasticSearchAdapter(
    private val localizablePlaceElasticRepository: LocalizablePlaceElasticRepository,
    private val localizablePlaceElasticMapper: LocalizablePlaceElasticMapper
) : LocalizablePlaceQueryRepositoryPort {

    override fun searchById(id: UUID): LocalizablePlaceModel? {

        return localizablePlaceElasticRepository.findById(id)
            .map { elasticEntity ->
                localizablePlaceElasticMapper.toDomainModel(elasticEntity)
            }
            .orElse(null)

    }

    override fun searchAll(): List<LocalizablePlaceModel> {

        return localizablePlaceElasticRepository.findAll().map { elasticEntity ->
            localizablePlaceElasticMapper.toDomainModel(elasticEntity)
        }.toList()

    }

    override fun searchByAliasStartingWith(alias: String): List<LocalizablePlaceModel> {
        return localizablePlaceElasticRepository.findByAliasStartingWith(alias).map { elasticEntity ->
            localizablePlaceElasticMapper.toDomainModel(elasticEntity)
        }.toList()
    }

    override fun searchByNearest(
        locationPointToSearch: Point,
        radiusInKm: Double
    ): List<LocalizablePlaceModel> {

        return localizablePlaceElasticRepository.findByLocationPointWithin(locationPointToSearch, radiusInKm).map { elasticEntity ->
            localizablePlaceElasticMapper.toDomainModel(elasticEntity)
        }.toList()

    }

    override fun synchronizeFromWriteRepository(sourceLocalizablePlacesList : List<LocalizablePlaceModel>?): Map<String, Int> {
        val resultMap: MutableMap<String, Int> = HashMap()

        resultMap["before.records-in-writeable-repository.count"] = sourceLocalizablePlacesList?.size ?: 0
        resultMap["before.entries-in-queryable-repository.count"] = localizablePlaceElasticRepository.count().toInt()

        // 1. Limpar todos os dados existentes no Elasticsearch
        localizablePlaceElasticRepository.deleteAll()

        // 3. Indexar os dados do PostgreSQL no Elasticsearch
        sourceLocalizablePlacesList?.let { localizablePlacesModelList ->
            val elasticEntitiesList = localizablePlacesModelList.map { oneLocalizablePlaceModel ->
                localizablePlaceElasticMapper.toElasticEntity(oneLocalizablePlaceModel)
            }
            val savedEntriesInReadRepository = this.localizablePlaceElasticRepository.saveAll(elasticEntitiesList)
            resultMap["after.entries-in-queryable-repository.count"] = savedEntriesInReadRepository.count().toInt()

        }

        println("Synchronization done: $resultMap")

        return resultMap
    }

    override fun removeAll() {
        localizablePlaceElasticRepository.deleteAll()
    }

    override fun searchByFriendlyIdContaining(friendlyId: String): List<LocalizablePlaceModel> {

        return localizablePlaceElasticRepository.findByFriendlyIdContaining(friendlyId).map { elasticEntity ->
            localizablePlaceElasticMapper.toDomainModel(elasticEntity)
        }.toList()

    }

    override fun searchByPropertiesDetailsMapContains(key: String, value: String): List<LocalizablePlaceModel> {

        return localizablePlaceElasticRepository.findByAdditionalDetailsMapContains(key, value).map { elasticEntity ->
            localizablePlaceElasticMapper.toDomainModel(elasticEntity)
        }.toList()

    }

    override fun searchByLocationAsGeoHash(geoHashToSearch: String): List<LocalizablePlaceModel> {

        return localizablePlaceElasticRepository.findByLocationAsGeoHashEndingWith(geoHashToSearch).map { elasticEntity ->
            localizablePlaceElasticMapper.toDomainModel(elasticEntity)
        }.toList()

    }
}