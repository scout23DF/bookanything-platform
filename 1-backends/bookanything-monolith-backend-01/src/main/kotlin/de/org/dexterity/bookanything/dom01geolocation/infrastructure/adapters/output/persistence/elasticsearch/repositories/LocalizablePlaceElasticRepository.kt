package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.repositories

import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.elasticsearch.entities.LocalizablePlaceElasticEntity
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LocalizablePlaceElasticRepository : ElasticsearchRepository<LocalizablePlaceElasticEntity, UUID>, LocalizablePlaceElasticRepositoryCustom {
    fun findByAliasStartingWith(searchedAlias: String): List<LocalizablePlaceElasticEntity>
    fun findByFriendlyIdContaining(friendlyId: String): List<LocalizablePlaceElasticEntity>
    fun findByAdditionalDetailsMapContains(key: String, value: String): List<LocalizablePlaceElasticEntity>
    fun findByLocationAsGeoHashEndingWith(geoHashToSearch: String): List<LocalizablePlaceElasticEntity>
}
