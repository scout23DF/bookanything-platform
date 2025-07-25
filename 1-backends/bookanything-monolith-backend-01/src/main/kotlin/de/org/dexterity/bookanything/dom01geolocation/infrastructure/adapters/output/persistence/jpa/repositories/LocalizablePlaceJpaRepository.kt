package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.repositories

import de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities.LocalizablePlaceJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface LocalizablePlaceJpaRepository : JpaRepository<LocalizablePlaceJpaEntity, UUID> {

    fun existsByName(name: String): Boolean

    fun findByFriendlyIdContainingIgnoreCase(friendlyId: String): List<LocalizablePlaceJpaEntity>

    @Query(value= "SELECT l.* FROM tb_localizable_place l WHERE l.js_properties_details ->> ?1 = ?2", nativeQuery = true)
    fun findByPropertiesDetailsMapContains(key: String, value: String): List<LocalizablePlaceJpaEntity>

    /*
    @Query(value = "SELECT * FROM centro_distribuicao WHERE ST_DWithin(localizacao, :point, :distanceInMeters, true)", nativeQuery = true)
    fun findByLocalizacaoWithinDistance(point: Point, distanceInMeters: Double): List<CentroDistribuicaoJpaEntity>
    */

}